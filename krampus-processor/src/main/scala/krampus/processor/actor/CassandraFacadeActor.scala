// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.FromConfig
import com.typesafe.config.Config
import krampus.entity.WikiEdit
import krampus.processor.cassandra.CassandraDao

/**
  * Actor to store entities in cassandra.
  */
class CassandraFacadeActor(config: Config, dao: CassandraDao[WikiEdit]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val editsDao = dao

  val wikiEditActor = context.actorOf(CassandraEntityActor.props[WikiEdit].withRouter(FromConfig()), "wiki-edit-actor")

  override def receive: Receive = {
    case Insert(entry) => {
      log.debug(s"Insert $entry into cassandra.")
      val back = sender
      wikiEditActor ! Store(entry, back)
    }

    case stored @ Stored(res) => {
      log.debug(s"$res stored in cassandra.")
      context.parent ! stored
    }

    case InvalidEntityType => log.error("Invalid entity type.")

    case msg => log.error(s"Unexpected message: $msg")
  }
}

object CassandraFacadeActor {
  def props(config: Config)(implicit dao: CassandraDao[WikiEdit]): Props = Props(new CassandraFacadeActor(config, dao))
}
