// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Controller, WebSocket}
import utils.AppConfig

import scala.concurrent.duration._

object ApplicationController extends Controller with LazyLogging {
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

  private[this] lazy val publisher: PublisherWithCommitSink[Array[Byte]] = reactiveKafka.consumeWithOffsetSink(consumerProperties)

  private[this] lazy val avroConverter = actorSystem.actorOf(AvroConverterActor.props(config))

  Source.fromPublisher(publisher.publisher)
    .map(processMessage)
    .to(publisher.offsetCommitSink).run()

  def stream(channel: String): WebSocket[String, JsValue] = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    logger.debug("Try to connect to the wiki channel '{}'", channel)
    KafkaActor.props(out)
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    avroConverter ! KMessage(msg.key(), msg.message())

    msg
  }

  def wikiwatch: Action[AnyContent] = Action { Ok(views.html.wikiwatch("WikiWatch")) }
}
