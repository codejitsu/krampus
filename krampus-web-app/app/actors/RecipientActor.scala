// Copyright (C) 2016, codejitsu.

package actors

import akka.actor._
import com.typesafe.scalalogging.LazyLogging

class RecipientActor(out: ActorRef, channel: String) extends Actor with LazyLogging {
  private[this] val wrapper: ActorRef = context.actorOf(ChannelListenerActor.props(out, channel))

  override def receive: Receive = {
    case "subscribe" =>
      logger.info("Received subscription from a client for channel '{}'", channel)
      RecipientActor.subscribe(wrapper)

    case "unsubscribe" =>
      logger.info("Received unsubscribe message from a client for channel '{}'", channel)

      RecipientActor.unsubscribe(wrapper)
      self ! PoisonPill
  }

  override def postStop(): Unit = {
    logger.info("Client unsubscribing from channel '{}'", channel)
  }
}

object RecipientActor extends LazyLogging {
  private[this] var subscribers = List[ActorRef]()

  def props(out: ActorRef, channel: String): Props = Props(new RecipientActor(out, channel))

  def unsubscribe(out: ActorRef): Unit = {
    subscribers = subscribers.filter(_ != out)
  }

  def subscribe(out: ActorRef): Unit = {
    subscribers = out :: subscribers
  }
}
