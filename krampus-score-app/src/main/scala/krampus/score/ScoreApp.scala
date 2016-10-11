// Copyright (C) 2016, codejitsu.

package krampus.score

import akka.actor.{ActorSystem, PoisonPill}
import com.typesafe.scalalogging.LazyLogging
import krampus.actor.protocol.StartStreamProcessor
import krampus.score.actor.NodeGuardianActor
import krampus.score.util.AppConfig

/**
  * Read Kafka Events and push all aggregated data into graphite.
  */
object ScoreApp extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus score app...")

    val appConfig = new AppConfig()

    logger.info(appConfig.config.root().render())

    val system = ActorSystem(appConfig.systemName)

    val guardian = system.actorOf(NodeGuardianActor.props(appConfig), "node-guardian")

    guardian ! StartStreamProcessor

    system.registerOnTermination {
      guardian ! PoisonPill
    }
  }
}
