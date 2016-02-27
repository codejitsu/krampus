// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.{Actor, Props, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.{StringKafkaMessage, KafkaMessage}
import com.typesafe.scalalogging.LazyLogging
import org.reactivestreams.Publisher
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Concurrent, Iteratee}
import scala.concurrent.duration._
import com.softwaremill.react.kafka.{PublisherWithCommitSink, ConsumerProperties, ReactiveKafka}
import kafka.serializer.{StringDecoder, Decoder}
import play.api.mvc.{WebSocket, AnyContent, Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent.Channel

import utils.AppConfig

class WebsocketPublisherActor(channel: Channel[String]) extends Actor with LazyLogging {
  override def receive: Receive = {
    case str: String =>
      logger.info(s"PublisherActor msg: $str")
      channel.push(str)
  }
}

class ApplicationController extends Controller with LazyLogging {
  private[this] val config = new AppConfig()
  private[this] lazy val reactiveKafka: ReactiveKafka = new ReactiveKafka()

  implicit val actorSystem = ActorSystem(config.systemName)
  implicit val materializer = ActorMaterializer.create(actorSystem)

  // consumer
  private[this] lazy val consumerProperties = ConsumerProperties(
    brokerList = config.kafkaConfig.getString("broker-list"),
    zooKeeperHost = config.kafkaConfig.getString("zookeeper-host"),
    topic = config.kafkaConfig.getString("topic"),
    groupId = config.kafkaConfig.getString("group-id"),
//    decoder = new Decoder[Array[Byte]] {
//      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
//    }
    decoder = new StringDecoder()
  ).commitInterval(1200 milliseconds)

  private[this] val publisher: Publisher[StringKafkaMessage] = reactiveKafka.consume(consumerProperties)

  def all: WebSocket[String, String] = {
    val (publicOut, publicChannel) = Concurrent.broadcast[String]
    val websocketActor = actorSystem.actorOf(Props(new WebsocketPublisherActor(publicChannel)))

    Source.fromPublisher(publisher).runForeach { m => websocketActor ! m.message() }

    WebSocket.using[String] {
      request =>
        val (privateOut, _) = Concurrent.broadcast[String]
        // val outEnumerator: Enumerator[String] = Streams.publisherToEnumerator(publisher).map(_.message())
        val out = Enumerator.interleave(publicOut, privateOut)
        (Iteratee.ignore[String], out)
    }
  }
}
