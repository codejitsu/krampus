// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.{ActorSystem, Props, ActorRef, Actor}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages._
import com.softwaremill.react.kafka.{PublisherWithCommitSink, ConsumerProperties, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import play.api.libs.json.Json
import utils.AppConfig
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

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
//    subscribers -= out
  }

  def subscribe(out: ActorRef): Unit = {
    subscribers += out
  }

  def subs(): ArrayBuffer[ActorRef] = subscribers
}
