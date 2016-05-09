// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import krampus.processor.util.AppConfig
import akka.actor.{Props, Actor}
import com.typesafe.scalalogging.LazyLogging

/**
  * Root actor node.
  */
class NodeGuardianActor(appConfig: AppConfig) extends Actor with LazyLogging {
  val kafkaListener = context.actorOf(KafkaListenerActor.props(appConfig), "kafka-listener")

  override def receive: Receive = {
    case StartListener =>
      kafkaListener ! InitializeListener

    case ListenerInitialized =>
      logger.info("Kafka listener initialized.")
  }
}

//TODO refactor all this stream processing, avro actors out to commons
object NodeGuardianActor {
  def props(config: AppConfig): Props = Props(new NodeGuardianActor(config))
}
