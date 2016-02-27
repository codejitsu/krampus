// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Action, AnyContent, Controller, WebSocket}
import play.api.Play.current
import utils.AppConfig

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

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
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  ).commitInterval(1200 milliseconds)

  private[this] val publisher: PublisherWithCommitSink[Array[Byte]] = reactiveKafka.consumeWithOffsetSink(consumerProperties)

  Source.fromPublisher(publisher.publisher)
    .map(processMessage)
    .to(publisher.offsetCommitSink).run()

  def all: WebSocket[String, String] = {
    val (publicOut, publicChannel) = Concurrent.broadcast[String]
    val websocketActor = actorSystem.actorOf(Props(new WebsocketPublisherActor(publicChannel)))
    val avroConverter = actorSystem.actorOf(AvroConverterActor.props(config, websocketActor))

    Source.fromPublisher(publisher.publisher)
      .map(processMessage(avroConverter))
      .to(publisher.offsetCommitSink).run()

    WebSocket.using[String] {
      request =>
        val (privateOut, _) = Concurrent.broadcast[String]
        val out = Enumerator.interleave(publicOut, privateOut)
        (Iteratee.ignore[String], out)
    }
  }

  private def processMessage(avroConverter: ActorRef)(msg: KafkaMessage[Array[Byte]]) = {
    logger.debug("Raw avro message: {}", new String(msg.message()))

    avroConverter ! RawKafkaMessage(msg.key(), msg.message())

    msg
  }

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index("Tweets"))
  }

  def wiki: WebSocket[String, JsValue] = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    KafkaStreamer.props(out, publisher)
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    val subscribers = KafkaActor.subs()

    //avroConverter ! RawKafkaMessage(msg.key(), msg.message())

    subscribers.foreach { sub =>
      sub ! Json.obj("text" -> "Hello, world!")
    }

    msg
  }

  def stream: WebSocket[String, JsValue] = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    KafkaActor.props(out)
  }
}
