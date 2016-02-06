// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{PublisherWithCommitSink, ConsumerProperties, ReactiveKafka}
import com.typesafe.config.Config
import kafka.serializer.Decoder
import scala.concurrent.duration._

/**
  * Actor to read all kafka events and propagate them to aggregate actors.
  */
class KafkaListenerActor(config: Config) extends Actor with ActorLogging with Protocol {
  import context.system
  implicit val materializer = ActorMaterializer.create(context.system)

  var consumerWithOffsetSink: PublisherWithCommitSink[Array[Byte]] = _

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

  def receive: Receive = {
    case InitializeReader =>
      log.info("Got InitializeReader message...")

      initListener()
      context.parent ! ReaderInitialized

    case Terminated(_) =>
      log.error("The consumer has been terminated, restarting the whole stream...")
      initListener()

    case _ =>
  }

  def initListener(): Unit = {
    consumerWithOffsetSink = new ReactiveKafka().consumeWithOffsetSink(consumerProperties)

    log.debug("Starting the kafka listener...")

    context.watch(consumerWithOffsetSink.publisherActor)

    Source.fromPublisher(consumerWithOffsetSink.publisher)
      .map(processMessage)
      .to(consumerWithOffsetSink.offsetCommitSink).run()
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    log.info("Msg.")

    msg
  }

  override def postStop(): Unit = {
    consumerWithOffsetSink.cancel()
    super.postStop()
  }
}

object KafkaListenerActor {
  def props(config: Config): Props = Props(new KafkaListenerActor(config))
}
