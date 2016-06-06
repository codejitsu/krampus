// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config

/**
  * Actor to store entities in cassandra.
  */
class CassandraActor(config: Config) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Insert(entry) =>
      log.debug(s"Insert $entry into cassandra.")

    case msg =>
      log.error(s"Unexpected message: $msg")
  }
}

object CassandraActor {
  def props(config: Config): Props = Props(new CassandraActor(config))
}
