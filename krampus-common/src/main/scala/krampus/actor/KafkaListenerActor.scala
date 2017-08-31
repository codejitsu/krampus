// Copyright (C) 2017, codejitsu.

package krampus.actor

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.config.Config
import kafka.serializer.Decoder
import krampus.actor.protocol.{InitializeQueueListener, QueueListenerInitialized}
import krampus.queue.RawKafkaMessage

import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * Actor to read all kafka events and propagate them to the next processing step.
  */
class KafkaListenerActor(config: Config, process: RawKafkaMessage => Unit) extends Actor with ActorLogging {
  import context.system

  implicit val materializer = ActorMaterializer.create(context.system)

  private[this] var consumerWithOffsetSink: Option[PublisherWithCommitSink[Array[Byte]]] = None

  private[this] lazy val reactiveKafka: ReactiveKafka = new ReactiveKafka()

  // consumer
  private[this] lazy val consumerProperties = ConsumerProperties(
    brokerList = config.getString("broker-list"),
    zooKeeperHost = config.getString("zookeeper-host"),
    topic = config.getString("topic"),
    groupId = config.getString("group-id"),
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  ).commitInterval(1200 milliseconds)
   .setProperty("zookeeper.connection.timeout.ms", config.getString("zookeeper-connection-timeout-ms"))
   .setProperty("zookeeper.session.timeout.ms", config.getString("zookeeper-session-timeout-ms"))

  override def receive: Receive = {
    case InitializeQueueListener =>
      val sndr: ActorRef = sender
      log.info("Received InitializeListener message.")
      initListener()
      sndr ! QueueListenerInitialized

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

    try {
      process(RawKafkaMessage(msg.key(), msg.message()))
    } catch {
      case NonFatal(e) => log.error(e, e.getMessage)
    }

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
  def props(config: Config, process: RawKafkaMessage => Unit): Props = Props(new KafkaListenerActor(config, process))
}
