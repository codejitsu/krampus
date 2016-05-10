// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{PublisherWithCommitSink, ConsumerProperties, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import krampus.processor.util.{AppConfig, RawKafkaMessage}
import scala.concurrent.duration._

/**
  * Actor to read all kafka events and propagate them to the next processing step.
  */
class KafkaListenerActor(config: AppConfig) extends Actor with LazyLogging {
  import context.system

  implicit val materializer = ActorMaterializer.create(context.system)

  private[this] var consumerWithOffsetSink: Option[PublisherWithCommitSink[Array[Byte]]] = None

  private[this] lazy val reactiveKafka: ReactiveKafka = new ReactiveKafka()

  // consumer
  private[this] lazy val consumerProperties = ConsumerProperties(
    brokerList = config.kafkaConfig.getString("broker-list"),
    zooKeeperHost = config.kafkaConfig.getString("zookeeper-host"),
    topic = config.kafkaConfig.getString("topic"),
    groupId = config.kafkaConfig.getString("group-id"),
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  ).commitInterval(1200 milliseconds)

  private[this] lazy val avroConverter = context.actorOf(AvroConverterActor.props(config), "avro-converter")

  override def receive: Receive = {
    case InitializeListener =>
      initListener()
      context.parent ! ListenerInitialized

    case MessageConverted => // avro converter successfully converted message

    case Terminated(_) =>
      logger.error("The consumer has been terminated, restarting the whole stream...")
      initListener()

    case msg =>
      logger.error(s"Unexpected message: $msg")
  }

  def initListener(): Unit = {
    consumerWithOffsetSink = Option(reactiveKafka.consumeWithOffsetSink(consumerProperties))

    consumerWithOffsetSink.foreach { consumer =>
      logger.info("Starting the kafka listener...")

      context.watch(consumer.publisherActor)

      Source.fromPublisher(consumer.publisher)
        .map(processMessage)
        .to(consumer.offsetCommitSink).run()
    }
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    avroConverter ! RawKafkaMessage(msg.key(), msg.message())

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
  def props(config: AppConfig): Props = Props(new KafkaListenerActor(config))
}
