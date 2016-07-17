// Copyright (C) 2016, codejitsu.

package krampus.processor

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import com.typesafe.scalalogging.LazyLogging
import krampus.entity.WikiEdit
import krampus.processor.actor.{CassandraFacadeActor, Insert, StartStreamProcessor, StreamProcessorActor}
import krampus.processor.cassandra.ProductionCassandraDatabaseProvider
import krampus.processor.util.AppConfig

/**
  * Read Kafka Events and store data in cassandra.
  */
object CassandraProcessorApp extends LazyLogging with ProductionCassandraDatabaseProvider {
  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus cassandra processor app...")

    val appConfig = new AppConfig("cassandra-processor-app")

    logger.info(appConfig.toString)

    val system = ActorSystem(appConfig.systemName)

    implicit val dao = database.WikiEdits

    val cassandraFacadeActor = system.actorOf(CassandraFacadeActor.props(appConfig.cassandraConfig), "cassandra-facade-actor")
    val streamProcessor = system.actorOf(StreamProcessorActor.props(appConfig, storeToCassandra(cassandraFacadeActor)), "stream-processor-actor")

    streamProcessor ! StartStreamProcessor

    system.registerOnTermination {
      streamProcessor ! PoisonPill
    }
  }

  def storeToCassandra(actor: ActorRef)(msg: WikiEdit): Unit = {
    logger.debug(s"About to store $msg to cassandra...")
    actor ! Insert(msg)
  }
}
