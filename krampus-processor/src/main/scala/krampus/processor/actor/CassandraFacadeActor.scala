// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.routing.FromConfig
import com.typesafe.config.Config
import krampus.entity.WikiEdit
import krampus.processor.cassandra.CassandraDao
import krampus.processor.util.AppConfig._

import scala.concurrent.duration.Duration

/**
  * Actor to store entities in cassandra.
  */
class CassandraFacadeActor(config: Config, dao: CassandraDao[WikiEdit]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val editsDao = dao

  val wikiEditActor = context.actorOf(CassandraEntityActor.props[WikiEdit].withRouter(FromConfig()), "wiki-edit-actor")

  private var inserted: Long = 0L

  val task: Option[Cancellable] = Some(context.system.scheduler.schedule(Duration.Zero, config.getMillis("flush-interval-ms")) {
    self ! PrintCountInserted
  })

  override def receive: Receive = {
    case Insert(entry) => {
      log.debug(s"Insert $entry into cassandra.")
      val back = sender
      wikiEditActor ! Store(entry, back)
    }

    case stored @ Stored(res) => {
      log.debug(s"$res stored in cassandra.")
      inserted = inserted + 1
      context.parent ! stored
    }

    case InvalidEntityType => log.error("Invalid entity type.")

    case PrintCountInserted => log.info(s"Total records inserted: $inserted")

    case msg => log.error(s"Unexpected message: $msg")
  }

  override def postStop(): Unit = {
    task.map(_.cancel())
    super.postStop()
  }
}

object CassandraFacadeActor {
  def props(config: Config)(implicit dao: CassandraDao[WikiEdit]): Props = Props(new CassandraFacadeActor(config, dao))
}
