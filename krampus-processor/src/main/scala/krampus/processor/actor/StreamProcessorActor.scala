// Copyright (C) 2017, codejitsu.

package krampus.processor.actor

import krampus.processor.util.AppConfig
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import krampus.actor.{AvroConverterActor, KafkaListenerActor}
import krampus.actor.protocol._
import krampus.entity.WikiEdit
import krampus.queue.RawKafkaMessage

/**
  * Stream processor.
  */
class StreamProcessorActor(appConfig: AppConfig, onMessage: WikiEdit => Unit) extends Actor with ActorLogging {
  private[this] val avroConverter = context.actorOf(AvroConverterActor.props(self), "avro-converter")
  private[this] val kafkaListener = context.actorOf(KafkaListenerActor.props(appConfig.kafkaConfig, processKafkaMessage),
    "kafka-listener")

  private[this] def processKafkaMessage(msg: RawKafkaMessage): Unit = avroConverter ! msg

  override def receive: Receive = {
    case StartStreamProcessor =>
      val sndr = sender()
      log.debug("Start initializing kafka listener.")
      kafkaListener ! InitializeQueueListener
      context.become(converter(sndr))
  }

  def converter(back: ActorRef): Receive = {
    case QueueListenerInitialized =>
      log.debug("Kafka listener initialized.")
      back ! StreamProcessorInitialized

    case MessageConverted(msg) =>
      log.debug(s"Message converted: $msg")
      onMessage(msg)
  }
}

//TODO refactor all this stream processing, avro actors out to commons
object StreamProcessorActor {
  def props(config: AppConfig, onMessage: WikiEdit => Unit): Props = Props(new StreamProcessorActor(config, onMessage))
}
