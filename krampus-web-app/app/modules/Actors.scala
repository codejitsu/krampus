// Copyright (C) 2016, codejitsu.

package modules

import javax.inject._

import actors.AvroConverterActor
import actors.Messages.KafkaRawDataMessage
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.google.inject.AbstractModule
import com.softwaremill.react.kafka.KafkaMessages._
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import kafka.serializer.Decoder
import utils.AppConfig

import scala.concurrent.duration._

trait ApplicationActors

class Actors @Inject()(system: ActorSystem) extends ApplicationActors {
  private[this] val config = new AppConfig()

  implicit val materializer = ActorMaterializer.create(system)

  private[this] val reactiveKafka: ReactiveKafka = new ReactiveKafka()

  // consumer
  private[this] val consumerProperties = ConsumerProperties(
    brokerList = config.kafkaConfig.getString("broker-list"),
    zooKeeperHost = config.kafkaConfig.getString("zookeeper-host"),
    topic = config.kafkaConfig.getString("topic"),
    groupId = config.kafkaConfig.getString("group-id"),
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  ).commitInterval(1200 milliseconds)

  private[this] val publisher: PublisherWithCommitSink[Array[Byte]] =
    reactiveKafka.consumeWithOffsetSink(consumerProperties)(system)

  private[this] val avroConverter = system.actorOf(AvroConverterActor.props(config))

  Source.fromPublisher(publisher.publisher)
    .map(processMessage)
    .to(publisher.offsetCommitSink).run()

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    avroConverter ! KafkaRawDataMessage(msg.message())

    msg
  }
}

class ActorsModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ApplicationActors])
      .to(classOf[Actors])
      .asEagerSingleton()
  }
}