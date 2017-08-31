// Copyright (C) 2017, codejitsu.

package actors

import actors.Messages.ChannelMessage
import akka.actor.{Props, Actor, ActorRef}
import com.typesafe.scalalogging.LazyLogging

class ChannelListenerActor(out: ActorRef, channel: String) extends Actor with LazyLogging {
  private[this] final val catchAll = "#all.wikipedia"

  override def receive: Receive = {
    case msg @ ChannelMessage(_, _) if catchAll == channel =>
      logger.info("Send message from {} to catch all websocket.", msg.channel)
      out ! msg.json

    case msg @ ChannelMessage(_, _) if msg.channel == channel =>
      logger.info("Send message from {} to websocket.", msg.channel)
      out ! msg.json

    case _ =>
  }

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[ChannelMessage])
  }
}

object ChannelListenerActor {
  def props(out: ActorRef, channel: String): Props = Props(new ChannelListenerActor(out, channel.toLowerCase))
}
