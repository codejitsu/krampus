// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{PublisherWithCommitSink, ConsumerProperties, ReactiveKafka}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import scala.concurrent.duration._

/**
  * Actor to read all kafka events and propagate them to aggregate actors.
  */
class KafkaListenerActor(config: Config) extends Actor with LazyLogging {
  import context.system
  implicit val materializer = ActorMaterializer.create(context.system)

  var consumerWithOffsetSink: Option[PublisherWithCommitSink[Array[Byte]]] = None

  // consumer
  val consumerProperties = ConsumerProperties(
    brokerList = config.getString("broker-list"),
    zooKeeperHost = config.getString("zookeeper-host"),
    topic = config.getString("topic"),
    groupId = config.getString("group-id"),
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  ).commitInterval(1200 milliseconds)

  override def receive: Receive = {
    case InitializeReader =>
      initListener()
      context.parent ! ReaderInitialized

    case Terminated(_) =>
      logger.error("The consumer has been terminated, restarting the whole stream...")
      initListener()

    case msg =>
      logger.error(s"Unexpected message: $msg")
  }

  def initListener(): Unit = {
    consumerWithOffsetSink = Option(new ReactiveKafka().consumeWithOffsetSink(consumerProperties))

    consumerWithOffsetSink.foreach { consumer =>
      logger.info("Starting the kafka listener...")

      context.watch(consumer.publisherActor)

      Source.fromPublisher(consumer.publisher)
        .map(processMessage)
        .to(consumer.offsetCommitSink).run()
    }
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    logger.info("Msg.")

    msg
  }

  override def postStop(): Unit = {
    consumerWithOffsetSink.foreach { consumer =>
      consumer.cancel()
    }

    super.postStop()
  }
}

object KafkaListenerActor {
  def props(config: Config): Props = Props(new KafkaListenerActor(config))
}
