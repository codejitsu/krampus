// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import com.typesafe.config.Config
import krampus.entity.WikiChangeEntry
import krampus.processor.cassandra.CassandraDao

/**
  * Actor to store entities in cassandra.
  */
class CassandraActor(config: Config, dao: CassandraDao[WikiChangeEntry]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher

  //TODO add all Cassandra actors here to store all parts of events.
  //TODO add tests to check if all parts stored correctly.

  override def receive: Receive = {
    case Insert(entry) => {
      log.debug(s"Insert $entry into cassandra.")

      val back = sender
      dao.store(entry)  map { res => StoreResult(entry, back) } pipeTo self
    }

    case StoreResult(res, to) => to ! Stored(res)

    case Failure(e) => log.error("Cassandra error", e)

    case msg => log.error(s"Unexpected message: $msg")
  }
}

object CassandraActor {
  def props(config: Config)(implicit dao: CassandraDao[WikiChangeEntry]): Props = Props(new CassandraActor(config, dao))
}
