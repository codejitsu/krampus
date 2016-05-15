// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import kafka.serializer.Decoder
import krampus.processor.util.AppConfig
import krampus.queue.RawKafkaMessage

import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * Actor to read all kafka events and propagate them to the next processing step.
  */
class KafkaListenerActor(config: AppConfig, process: RawKafkaMessage => Unit) extends Actor with ActorLogging {
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

  override def receive: Receive = {
    case InitializeListener =>
      log.info("Received InitializeListener message.")
      initListener()
      context.parent ! ListenerInitialized

    case Terminated(_) =>
      log.error("The consumer has been terminated, restarting the whole stream...")
      initListener()

    case msg =>
      log.error(s"Unexpected message: $msg")
  }

  def initListener(): Unit = try {
    consumerWithOffsetSink = Option(reactiveKafka.consumeWithOffsetSink(consumerProperties))

    consumerWithOffsetSink.foreach { consumer =>
      log.info("Starting the kafka listener...")

      context.watch(consumer.publisherActor)

      Source.fromPublisher(consumer.publisher)
        .map(processMessage)
        .to(consumer.offsetCommitSink).run()
    }
  } catch {
    case NonFatal(e) => log.error(e, e.getMessage)
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    log.debug(s"Before process message $msg")

    process(RawKafkaMessage(msg.key(), msg.message()))

    log.debug(s"After process message $msg")

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
  def props(config: AppConfig, process: RawKafkaMessage => Unit): Props = Props(new KafkaListenerActor(config, process))
}
