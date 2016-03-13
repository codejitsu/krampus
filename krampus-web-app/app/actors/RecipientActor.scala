// Copyright (C) 2016, codejitsu.

package actors

import akka.actor._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

class RecipientActor(out: ActorRef, channel: String) extends Actor with LazyLogging {
  private[this] var wrapper: Option[ActorRef] = None

  override def receive: Receive = {
    case "subscribe" =>
      logger.info("Received subscription from a client")

      wrapper = Option(context.actorOf(ChannelListenerActor.props(out, channel)))

      wrapper.foreach { wrap =>
        RecipientActor.subscribe(wrap)
      }

    case "unsubscribe" =>
      logger.info("Received unsubscribe message from a client")

      wrapper.foreach { wrap =>
        wrap ! PoisonPill
      }

      self ! PoisonPill
  }

  override def postStop(): Unit = {
    logger.info("Client unsubscribing from stream")

    wrapper.foreach { wrap =>
      RecipientActor.unsubscribe(wrap)
    }
  }
}

object RecipientActor extends LazyLogging {
  private[this] var subscribers = ArrayBuffer[ActorRef]()

  def props(out: ActorRef, channel: String): Props = Props(new RecipientActor(out, channel))

  def unsubscribe(out: ActorRef): Unit = {
    subscribers -= out
  }

  def subscribe(out: ActorRef): Unit = {
    subscribers += out
  }

  def subs(): ArrayBuffer[ActorRef] = subscribers
}