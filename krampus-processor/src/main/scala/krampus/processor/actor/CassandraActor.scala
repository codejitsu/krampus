// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config
import krampus.entity.WikiChangeEntry
import krampus.processor.cassandra.CassandraDao

/**
  * Actor to store entities in cassandra.
  */
class CassandraActor(config: Config, dao: CassandraDao[WikiChangeEntry]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val changeEntryDao = dao

  val changeEntityActor = context.actorOf(CassandraEntityActor.props[WikiChangeEntry])

  override def receive: Receive = {
    case Insert(entry) => {
      log.debug(s"Insert $entry into cassandra.")
      val back = sender
      changeEntityActor ! Store(entry, back)
    }

    case stored @ Stored(res) => {
      log.debug(s"$res stored in cassandra.")
      context.parent ! stored
    }

    case InvalidEntityType => log.error("Invalid entity type.")

    case msg => log.error(s"Unexpected message: $msg")
  }
}

object CassandraActor {
  def props(config: Config)(implicit dao: CassandraDao[WikiChangeEntry]): Props = Props(new CassandraActor(config, dao))
}
