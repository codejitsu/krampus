// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

class KafkaActor(out: ActorRef) extends Actor with LazyLogging {
  override def receive: Receive = {
    case "subscribe" =>
      logger.info("Received subscription from a client")
      KafkaActor.subscribe(out)
  }

  override def postStop(): Unit = {
    logger.info("Client unsubscribing from stream")
    KafkaActor.unsubscribe(out)
  }
}

object KafkaActor extends LazyLogging {
  private[this] var subscribers = ArrayBuffer[ActorRef]()

  def props(out: ActorRef): Props = Props(new KafkaActor(out))

  def unsubscribe(out: ActorRef): Unit = {
    subscribers -= out
  }

  def subscribe(out: ActorRef): Unit = {
    subscribers += out
  }

  def subs(): ArrayBuffer[ActorRef] = subscribers
}
