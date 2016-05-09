// Copyright (C) 2016, codejitsu.

package krampus.processor

import akka.actor.{ActorSystem, PoisonPill}
import com.typesafe.scalalogging.LazyLogging
import krampus.processor.actor.{StartListener, NodeGuardianActor}
import krampus.processor.util.AppConfig

/**
  * Read Kafka Events and store data in cassandra.
  */
object CassandraProcessorApp extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus cassandra processor app...")

    val appConfig = new AppConfig()
    val system = ActorSystem(appConfig.systemName)

    val guardian = system.actorOf(NodeGuardianActor.props(appConfig), "node-guardian")

    guardian ! StartListener

    system.registerOnTermination {
      guardian ! PoisonPill
    }
  }
}
