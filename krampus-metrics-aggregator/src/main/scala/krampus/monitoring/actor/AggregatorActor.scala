// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Cancellable, Props, Actor}
import krampus.entity.WikiChangeEntry
import krampus.monitoring.util.AggregationMessage

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Makes some aggregates from raw events.
  */

//TODO add specific aggregators like CounterAggregatorActor, etc...
class AggregatorActor(flushInterval: FiniteDuration,
                      agg: WikiChangeEntry => Unit,
                      flush: => Unit) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  val task: Option[Cancellable] = Some(context.system.scheduler.schedule(Duration.Zero, flushInterval) {
    self ! Flush
  })

  override def receive: Receive = {
    case AggregationMessage(msg) => agg(msg)
    case Flush => flush
  }

  override def postStop(): Unit = {
    task.map(_.cancel())
    super.postStop()
  }
}

object AggregatorActor {
  def props(flushInterval: FiniteDuration, agg: WikiChangeEntry => Unit,
            flush: => Unit): Props = Props(new AggregatorActor(flushInterval, agg, flush))
}
