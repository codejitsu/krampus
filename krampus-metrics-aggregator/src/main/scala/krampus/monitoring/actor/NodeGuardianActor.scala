// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Actor}
import com.typesafe.scalalogging.LazyLogging
import krampus.monitoring.util.AppConfig

/**
  * Root actor node.
  */
class NodeGuardianActor extends Actor with LazyLogging {
  val appConfig = new AppConfig()

  val kafkaListener = context.actorOf(KafkaListenerActor.props(appConfig.kafkaConfig), "kafka-listener")

  override def preStart(): Unit = {
    super.preStart()

    kafkaListener ! InitializeReader
  }

  override def receive: Receive = {
    case ReaderInitialized =>
      logger.info("Kafka listener initialized.")
  }
}

object NodeGuardianActor {
  def props(): Props = Props(new NodeGuardianActor)
}
