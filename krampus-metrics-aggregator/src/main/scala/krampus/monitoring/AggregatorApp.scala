// Copyright (C) 2017, codejitsu.

package krampus.monitoring

import akka.actor.{ActorSystem, PoisonPill}
import com.typesafe.scalalogging.LazyLogging
import krampus.actor.protocol.StartStreamProcessor
import krampus.monitoring.actor.NodeGuardianActor
import krampus.monitoring.util.AppConfig

/**
  * Read Kafka Events and push all aggregated data into graphite.
  */
object AggregatorApp extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus aggregator app...")

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
