// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, Props}

import scala.reflect.ClassTag

class CassandraEntityActor[E](implicit ev: ClassTag[E]) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Store(e) if e.getClass == ev.runtimeClass =>
      log.debug(s"Processing message $e of class ${ev.runtimeClass.getCanonicalName}")
      sender ! Stored(e)
    }
}

object CassandraEntityActor {
  def props[E](implicit ev: ClassTag[E]): Props = Props(new CassandraEntityActor[E])
}