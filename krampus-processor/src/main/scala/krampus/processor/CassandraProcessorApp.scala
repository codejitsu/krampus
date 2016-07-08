// Copyright (C) 2016, codejitsu.

package krampus.processor

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import com.typesafe.scalalogging.LazyLogging
import krampus.entity.WikiChangeEntry
import krampus.processor.actor.{CassandraActor, Insert, StartStreamProcessor, StreamProcessorActor}
import krampus.processor.cassandra.ProductionCassandraDatabaseProvider
import krampus.processor.util.AppConfig

/**
  * Read Kafka Events and store data in cassandra.
  */
object CassandraProcessorApp extends LazyLogging with ProductionCassandraDatabaseProvider {
  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus cassandra processor app...")

    val appConfig = new AppConfig("cassandra-processor-app")
    val system = ActorSystem(appConfig.systemName)

    implicit val dao = database.Edits

    val cassandraActor = system.actorOf(CassandraActor.props(appConfig.cassandraConfig), "cassandra-actor")
    val streamProcessor = system.actorOf(StreamProcessorActor.props(appConfig, storeToCassandra(cassandraActor)), "stream-processor-actor")

    streamProcessor ! StartStreamProcessor

    system.registerOnTermination {
      streamProcessor ! PoisonPill
    }
  }

  def storeToCassandra(actor: ActorRef)(msg: WikiChangeEntry): Unit = {
    logger.debug(s"About to store $msg to cassandra...")
    actor ! Insert(msg)
  }
}
