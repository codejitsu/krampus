// Copyright (C) 2016, codejitsu.

package krampus.monitoring

import akka.actor.{PoisonPill, ActorSystem}
import com.typesafe.scalalogging.LazyLogging
import krampus.monitoring.actor.{StartListener, NodeGuardianActor}
import krampus.monitoring.util.AppConfig

/**
  * Read Kafka Events and push all aggregated data into graphite.
  */
object AggregatorApp extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus aggregator app...")

    val appConfig = new AppConfig()

    logger.info(appConfig.toString)

    val system = ActorSystem(appConfig.systemName)

    val guardian = system.actorOf(NodeGuardianActor.props(appConfig), "node-guardian")

    guardian ! StartListener

    system.registerOnTermination {
      guardian ! PoisonPill
    }
  }
}
