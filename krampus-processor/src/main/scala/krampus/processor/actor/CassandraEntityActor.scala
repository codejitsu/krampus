// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import krampus.processor.cassandra.CassandraDao

import scala.reflect.ClassTag

class CassandraEntityActor[E](implicit ev: ClassTag[E], dao: CassandraDao[E]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case Store(e) if e.getClass == ev.runtimeClass => {
      log.debug(s"Processing message $e of class ${ev.runtimeClass.getCanonicalName}")

      val concreteEntity = e.asInstanceOf[E]
      val back = sender

      dao.store(concreteEntity) map { res => StoreResult(concreteEntity, back) } pipeTo self
    }

    case Store(_) => sender ! InvalidEntityType

    case StoreResult(r, to) => to ! Stored(r.asInstanceOf[E])

    case Failure(e) => log.error("Cassandra error", e)
  }
}

object CassandraEntityActor {
  def props[E](implicit ev: ClassTag[E], dao: CassandraDao[E]): Props = Props(new CassandraEntityActor[E])
}
