// Copyright (C) 2016, codejitsu.

package krampus.monitoring

import akka.actor.{PoisonPill, ActorSystem}
import com.typesafe.scalalogging.LazyLogging
import krampus.monitoring.actor.NodeGuardianActor

/**
  * Read Kafka Events and push all aggregated data into graphite.
  */
object AggregatorApp extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus aggregator app...")

    val system = ActorSystem("Krampus-System")

    val guardian = system.actorOf(NodeGuardianActor.props(), "node-guardian")

    system.registerOnTermination {
      guardian ! PoisonPill
    }
  }
}
