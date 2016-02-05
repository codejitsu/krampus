// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Actor}

/**
  * Root actor node.
  */
class NodeGuardianActor extends Actor {
  val api = context.actorOf(KafkaListenerActor.props(), "kafka-listener")

  override def receive: Receive = {
    case _ =>
  }
}

object NodeGuardianActor {
  def props(): Props = Props(new NodeGuardianActor)
}
