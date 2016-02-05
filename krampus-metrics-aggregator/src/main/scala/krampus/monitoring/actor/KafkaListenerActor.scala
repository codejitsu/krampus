// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{ActorRef, Props, Actor}
import akka.stream.ActorMaterializer
import com.softwaremill.react.kafka.{ConsumerProperties, ReactiveKafka}
import kafka.serializer.Decoder

/**
  * Actor to read all kafka events and propagate them to aggregate actors.
  */
class KafkaListenerActor extends Actor {
  import context.system
  implicit val materializer = ActorMaterializer.create(context.system)

  val kafka = new ReactiveKafka()

  // consumer
  val consumerProperties = ConsumerProperties(
    brokerList = "localhost:9092",
    zooKeeperHost = "localhost:2181",
    topic = "lowercaseStrings",
    groupId = "groupName",
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  )

  val topLevelPublisherActor: ActorRef = kafka.consumerActor(consumerProperties)

  def receive: Receive = {
    case _ =>
  }
}

object KafkaListenerActor {
  def props(): Props = Props(new KafkaListenerActor)
}
