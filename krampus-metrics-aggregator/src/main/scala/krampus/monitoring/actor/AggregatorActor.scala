// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Actor}

/**
  * Makes some aggregates from raw events.
  */
class AggregatorActor extends Actor {
  override def receive: Receive = {
    case _ =>
  }
}

object AggregatorActor {
  def props(): Props = Props(new AggregatorActor)
}
