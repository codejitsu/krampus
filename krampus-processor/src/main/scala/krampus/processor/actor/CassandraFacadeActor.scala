// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.FromConfig
import com.typesafe.config.Config
import krampus.actor.protocol._
import krampus.entity.WikiEdit
import krampus.processor.cassandra.CassandraDao

/**
  * Actor to store entities in cassandra.
  */
class CassandraFacadeActor(config: Config, dao: CassandraDao[WikiEdit], caller: Option[ActorRef]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val editsDao = dao

  val wikiEditActor = context.actorOf(CassandraEntityActor.props[WikiEdit].withRouter(FromConfig()), "wiki-edit-actor")

  private var inserted: Long = 0L

  override def receive: Receive = {
    case Insert(entry) => {
      log.debug(s"Insert $entry into cassandra.")
      wikiEditActor ! Store(entry, self, caller)
    }

    case stored @ Stored(res, caller) => {
      log.debug(s"$res stored in cassandra.")
      inserted = inserted + 1

      caller.foreach { c =>
        c ! stored
      }
    }

    case InvalidEntityType => log.error("Invalid entity type.")

    case GetCountInserted => {
      val back = sender()
      back ! CountInserted(inserted)
    }

    case msg => log.error(s"Unexpected message: $msg")
  }
}

object CassandraFacadeActor {
  def props(config: Config, caller: Option[ActorRef])(implicit dao: CassandraDao[WikiEdit]): Props = Props(new CassandraFacadeActor(config, dao, caller))
}
