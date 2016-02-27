// Copyright (C) 2016, codejitsu.

package controllers

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.PublisherWithCommitSink
import com.typesafe.scalalogging.LazyLogging
import play.api._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Broadcaster
import play.api.libs.json._
import play.extras.iteratees._
import play.api.libs.concurrent.Execution.Implicits._

import scala.collection.mutable.ArrayBuffer

class KafkaStreamer(out: ActorRef) extends Actor {
  def receive: Receive = {
    case "subscribe" =>
      Logger.info("Received subscription from a client")
      KafkaStreamer.subscribe(out)
  }

  override def postStop() {
    Logger.info("Client unsubscribing from stream")
    KafkaStreamer.unsubscribe(out)
  }
}

object KafkaStreamer extends LazyLogging {
  implicit val actorSystem = ActorSystem("KafkaStreamer")
  implicit val materializer = ActorMaterializer.create(actorSystem)

  private var broadcastEnumerator: Option[Enumerator[JsObject]] = None

  private var broadcaster: Option[Broadcaster] = None

  private val subscribers = new ArrayBuffer[ActorRef]()

  private var kafkaPublisher: PublisherWithCommitSink[Array[Byte]] = _

  def props(out: ActorRef, publisher: PublisherWithCommitSink[Array[Byte]]): Props = {
    kafkaPublisher = publisher

    Props(new KafkaStreamer(out))
  }

  def subscribe(out: ActorRef): Unit = {
    if (broadcastEnumerator == None) {
      init()
    }

    def kafkaClient: Iteratee[JsObject, Unit] = Cont {
      case Input.EOF => Done(None)
      case Input.El(o) =>
        if (subscribers.contains(out)) {
          out ! o
          kafkaClient
        } else {
          Done(None)
        }
      case Input.Empty =>
        kafkaClient
    }

    broadcastEnumerator.map { enumerator =>
      enumerator run kafkaClient
    }

    subscribers += out
  }

  def unsubscribe(subscriber: ActorRef): Unit = {
      val index = subscribers.indexWhere(_ == subscriber)
      if (index > 0) {
        subscribers.remove(index)
        Logger.info("Unsubscribed client from stream")
      }
  }

  def subscribeNode: Enumerator[JsObject] = {
    if (broadcastEnumerator == None) {
      KafkaStreamer.init()
    }

    broadcastEnumerator.getOrElse {
      Enumerator.empty[JsObject]
    }
  }

  def init(): Unit = {
    val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]

    val jsonStream: Enumerator[JsObject] = enumerator &>
      Encoding.decode() &>
      Enumeratee.grouped(JsonIteratees.jsSimpleObject)

    val (e, b) = Concurrent.broadcast(jsonStream)

    broadcastEnumerator = Some(e)
    broadcaster = Some(b)

    Source.fromPublisher(kafkaPublisher.publisher).runForeach { msg =>
      logger.debug("Kafka msg: {}", msg)
     // iteratee
    }
  }
}
