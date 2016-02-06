// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{ActorLogging, Props, Actor}
import krampus.monitoring.util.AppConfig

/**
  * Root actor node.
  */
class NodeGuardianActor extends Actor with ActorLogging with Protocol {
  val appConfig = new AppConfig()

  val kafkaListener = context.actorOf(KafkaListenerActor.props(appConfig.kafkaConfig), "kafka-listener")

  kafkaListener ! InitializeReader

  override def receive: Receive = {
    case ReaderInitialized =>
      log.info("Kafka listener initialized.")
  }
}

object NodeGuardianActor {
  def props(): Props = Props(new NodeGuardianActor)
}
