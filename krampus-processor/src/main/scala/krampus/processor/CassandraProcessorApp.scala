// Copyright (C) 2016, codejitsu.

package krampus.processor

import akka.actor.{ActorRef, ActorSystem, Cancellable, PoisonPill}
import akka.pattern.ask
import com.typesafe.scalalogging.LazyLogging
import krampus.entity.WikiEdit
import krampus.processor.actor._
import krampus.processor.cassandra.ProductionCassandraDatabaseProvider
import krampus.processor.util.AppConfig
import krampus.processor.util.AppConfig._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Read Kafka Events and store data in cassandra.
  */
object CassandraProcessorApp extends LazyLogging with ProductionCassandraDatabaseProvider {
  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus cassandra processor app...")

    val appConfig = new AppConfig("cassandra-processor-app")

    logger.info(appConfig.config.toString)

    val system = ActorSystem(appConfig.systemName)

    implicit val dao = database.WikiEdits
    implicit val ec = system.dispatcher
    implicit val timeout = akka.util.Timeout(10 seconds)

    val cassandraFacadeActor = system.actorOf(CassandraFacadeActor.props(appConfig.cassandraConfig), "cassandra-facade-actor")
    val streamProcessor = system.actorOf(StreamProcessorActor.props(appConfig, storeToCassandra(cassandraFacadeActor)), "stream-processor-actor")

    val task: Option[Cancellable] = Some(system.scheduler.schedule(Duration.Zero, appConfig.cassandraConfig.getMillis("flush-interval-ms")) {
      val insertedFut: Future[Long] = (cassandraFacadeActor ? GetCountInserted).mapTo[Long]

      insertedFut.onSuccess { case inserted =>
        logger.info(s"Inserted entries into Cassandra: # $inserted")
      }
    })

    streamProcessor ! StartStreamProcessor

    system.registerOnTermination {
      streamProcessor ! PoisonPill
      task.map(_.cancel())
    }
  }

  def storeToCassandra(actor: ActorRef)(msg: WikiEdit): Unit = {
    logger.debug(s"About to store $msg to cassandra...")
    actor ! Insert(msg)
  }
}
