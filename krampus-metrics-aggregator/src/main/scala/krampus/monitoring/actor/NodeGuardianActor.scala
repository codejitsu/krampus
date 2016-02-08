// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Actor}
import com.typesafe.scalalogging.LazyLogging
import krampus.monitoring.util.AppConfig

/**
  * Root actor node.
  */
class NodeGuardianActor(appConfig: AppConfig) extends Actor with LazyLogging {
  val kafkaListener = context.actorOf(KafkaListenerActor.props(appConfig.kafkaConfig), "kafka-listener")

  override def receive: Receive = {
    case InitReader =>
      kafkaListener ! InitializeReader

    case ReaderInitialized =>
      logger.info("Kafka listener initialized.")
  }
}

object NodeGuardianActor {
  def props(config: AppConfig): Props = Props(new NodeGuardianActor(config))
}
