// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import krampus.actor.protocol.{InitializeQueueListener, MessageConverted, QueueListenerInitialized, StartStreamProcessor}
import krampus.actor.{AvroConverterActor, KafkaListenerActor}
import krampus.entity.WikiEdit
import krampus.monitoring.util.{AggregationMessage, AppConfig}
import krampus.queue.RawKafkaMessage
import krampus.monitoring.util.AppConfig.ConfigDuration
import scala.collection.mutable

/**
  * Root actor node.
  */
class NodeGuardianActor(config: AppConfig) extends Actor with ActorLogging {
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

  private[this] val avroConverter = context.actorOf(AvroConverterActor.props(self), "avro-converter")
  private[this] val kafkaListener = context.actorOf(KafkaListenerActor.props(config.kafkaConfig, processKafkaMessage),
    "kafka-listener")

  private[this] def processKafkaMessage(msg: RawKafkaMessage): Unit = avroConverter ! msg

  override def receive: Receive = {
    case StartStreamProcessor =>
      log.debug("Start initializing kafka listener.")
      kafkaListener ! InitializeQueueListener

    case QueueListenerInitialized =>
      log.info("Kafka listener initialized.")

    case MessageConverted(msg) =>
      log.debug(s"Message converted: $msg")
      onMessage(msg)
  }

  def onMessage(msg: WikiEdit): Unit = {
    val aggMsg = AggregationMessage(msg)

    val channelCounter = counters.getOrElseUpdate(msg.channel,
      context.actorOf(CounterActor.props[AggregationMessage](msg.channel,
        config.aggregationConfig.getMillis("flush-interval-ms"),
        e => e.msg.channel == msg.channel, statsdGateway), s"${msg.channel.drop(1)}-counter"))

    allCounter ! aggMsg
    channelCounter ! aggMsg
  }
}

object NodeGuardianActor {
  def props(config: AppConfig): Props = Props(new NodeGuardianActor(config))
}
