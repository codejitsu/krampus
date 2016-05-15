// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import krampus.processor.util.AppConfig
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.queue.RawKafkaMessage

/**
  * Root actor node.
  */
class NodeGuardianActor(appConfig: AppConfig) extends Actor with LazyLogging {
  val avroConverter = context.actorOf(AvroConverterActor.props(self), "avro-converter")
  val kafkaListener = context.actorOf(KafkaListenerActor.props(appConfig.kafkaConfig, processKafkaMessage),
    "kafka-listener")

  override def receive: Receive = {
    case StartListener =>
      kafkaListener ! InitializeListener

    case ListenerInitialized =>
      logger.info("Kafka listener initialized.")

    case MessageConverted(msg) =>
      logger.info(s"Message converted: $msg")
  }

  def processKafkaMessage(msg: RawKafkaMessage): Unit = avroConverter ! msg
}

//TODO refactor all this stream processing, avro actors out to commons
object NodeGuardianActor {
  def props(config: AppConfig): Props = Props(new NodeGuardianActor(config))
}
