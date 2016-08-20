// Copyright (C) 2016, codejitsu.

package krampus.score.actor

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.score.util.AppConfig

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

object NodeGuardianActor {
  def props(config: AppConfig): Props = Props(new NodeGuardianActor(config))
}
