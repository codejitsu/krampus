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
    case Store(e, back, caller) if e.getClass == ev.runtimeClass => {
      log.debug(s"Processing message $e of class ${ev.runtimeClass.getCanonicalName}")
      val concreteEntity = e.asInstanceOf[E]
      dao.store(concreteEntity) map { res => StoreResult(concreteEntity, back, caller) } pipeTo self
    }

    case Store(_, back, _) => back ! InvalidEntityType

    case StoreResult(res, back, caller) => back ! Stored(res, caller)

    case Failure(e) => log.error(e, "Cassandra error")
  }
}

object CassandraEntityActor {
  def props[E](implicit ev: ClassTag[E], dao: CassandraDao[E]): Props = Props(new CassandraEntityActor[E])
}
