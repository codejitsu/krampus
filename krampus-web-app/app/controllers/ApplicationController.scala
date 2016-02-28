// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.{Actor, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import play.api.Play.current
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Controller, WebSocket}
import utils.AppConfig

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

  private[this] val avroConverter = actorSystem.actorOf(AvroConverterActor.props(config))

  Source.fromPublisher(publisher.publisher)
    .map(processMessage)
    .to(publisher.offsetCommitSink).run()

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index("Wikipedia Stream"))
  }

  def stream: WebSocket[String, JsValue] = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    KafkaActor.props(out)
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    avroConverter ! KMessage(msg.key(), msg.message())

    msg
  }
}
