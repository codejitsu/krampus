// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.Actor

/**
  * Actor to read all kafka events and propagate them to aggregate actors.
  */
class KafkaListenerActor extends Actor {
  def receive: Receive = {
    case _ =>
  }
}
