// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import krampus.actor.AvroConverterActor
import krampus.actor.protocol.MessageConverted
import krampus.monitoring.util.{AggregationMessage, AppConfig}
import krampus.monitoring.util.AppConfig.ConfigDuration
import krampus.queue.RawKafkaMessage

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Actor to read all kafka events and propagate them to aggregate actors.
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

  private[this] lazy val statsdConnection =
    (config.aggregationConfig.getString("statsd.host"), config.aggregationConfig.getInt("statsd.port"))

  private[this] val statsdGateway =
    new StatsD(context, statsdConnection._1, statsdConnection._2,
      packetBufferSize = config.aggregationConfig.getInt("statsd.packet-buffer-size"))

  private[this] lazy val allCounter =
    context.actorOf(CounterActor.props[AggregationMessage]("all-messages",
      config.aggregationConfig.getMillis("flush-interval-ms"),
      _ => true, statsdGateway), "all-messages-counter")

  private[this] lazy val counters: mutable.Map[String, ActorRef] = mutable.Map.empty

  private[this] lazy val avroConverter = context.actorOf(AvroConverterActor.props(self), "avro-converter")

  override def receive: Receive = {
    case InitializeListener =>
      initListener()
      context.parent ! ListenerInitialized

    case MessageConverted(entry) => // avro converter successfully converted message
      val aggMsg = AggregationMessage(entry)

      val channelCounter = counters.getOrElseUpdate(entry.channel,
        context.actorOf(CounterActor.props[AggregationMessage](entry.channel,
          config.aggregationConfig.getMillis("flush-interval-ms"),
          e => e.msg.channel == entry.channel, statsdGateway), s"${entry.channel.drop(1)}-counter"))

      allCounter ! aggMsg
      channelCounter ! aggMsg

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
