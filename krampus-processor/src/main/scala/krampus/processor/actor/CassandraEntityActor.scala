// Copyright (C) 2017, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, Props}
import krampus.actor.protocol.{InvalidEntityType, Store, StoreResult, Stored}
import krampus.processor.cassandra.CassandraDao

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class CassandraEntityActor[E](implicit ev: ClassTag[E], dao: CassandraDao[E]) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher

  var thresholdErrors = 10000 // scalastyle:ignore

  override def receive: Receive = {
    case store @ Store(e, back, caller) if e.getClass == ev.runtimeClass => {
      log.debug(s"Processing message $e of class ${ev.runtimeClass.getCanonicalName}")
      val concreteEntity = e.asInstanceOf[E]
      val storeF = dao.store(concreteEntity)

      storeF onComplete {
        case Success(r) => self ! StoreResult(concreteEntity, back, caller)
        case Failure(th) => {
          thresholdErrors = thresholdErrors - 1

          if (thresholdErrors < 0) {
            log.error("Too many cassandra errors.")
          }

          self ! store
        }
      }
    }

    case Store(_, back, _) => back ! InvalidEntityType

    case StoreResult(res, back, caller) => back ! Stored(res, caller)

    case Failure(e) => log.error(e, "Cassandra error")
  }
}

object CassandraEntityActor {
  def props[E](implicit ev: ClassTag[E], dao: CassandraDao[E]): Props = Props(new CassandraEntityActor[E])
}
