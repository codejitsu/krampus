// Copyright (C) 2016, codejitsu.

package krampus.monitoring

import akka.actor.{PoisonPill, ActorSystem}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import krampus.monitoring.actor.NodeGuardianActor

/**
  * Read Kafka Events and push all aggregated data into graphite.
  */
object AggregatorApp extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("KrampusSystem", ConfigFactory.parseString("akka.remote.netty.tcp.port = 2552"))
    val guardian = system.actorOf(NodeGuardianActor.props(), "node-guardian")

    system.registerOnTermination {
      guardian ! PoisonPill
    }

    logger.info("Created NodeGuardianActor.")
  }
}
