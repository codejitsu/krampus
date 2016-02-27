// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._
import com.softwaremill.react.kafka.{PublisherWithCommitSink, ConsumerProperties, ReactiveKafka}
import kafka.serializer.Decoder
import play.api.mvc.{AnyContent, Action, Controller}
import utils.AppConfig

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

  private[this] val consumerWithOffsetSink: Option[PublisherWithCommitSink[Array[Byte]]] =
    Option(reactiveKafka.consumeWithOffsetSink(consumerProperties))

  consumerWithOffsetSink.foreach { consumer =>
    logger.info("Starting the kafka listener...")

    Source.fromPublisher(consumer.publisher)
      .map(processMessage)
      .to(consumer.offsetCommitSink).run()
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    logger.debug(s"Kafka message: key with ${msg.key().length} bytes, value with ${msg.message().length} bytes.")

    msg
  }

  def index: Action[AnyContent] = Action {
    Ok("OK!")
  }
}
