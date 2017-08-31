// Copyright (C) 2017, codejitsu.

package actors

import java.net.URL
import java.util.UUID

import actors.Messages.ChannelMessage
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.actor.AvroConverterActor
import krampus.actor.protocol.MessageConverted
import krampus.entity.WikiEdit
import krampus.queue.RawKafkaMessage
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Writes}

import scala.util.{Failure, Try}

class ChannelPublisherActor extends Actor with LazyLogging {
  implicit val uuidWrites = new Writes[UUID] {
    def writes(uuid: UUID) = Json.toJson(uuid.toString)
  }

  implicit val urlWrites = new Writes[URL] {
    def writes(url: URL) = Json.toJson(url.toString)
  }

  implicit val wikiEditWrites: Writes[WikiEdit] = (
    (JsPath \ "id").write[UUID] and
      (JsPath \ "isRobot").write[Boolean] and
      (JsPath \ "channel").write[String] and
      (JsPath \ "timestamp").write[DateTime] and
      (JsPath \ "flags").write[List[String]] and
      (JsPath \ "isUnpatrolled").write[Boolean] and
      (JsPath \ "page").write[String] and
      (JsPath \ "diffUrl").write[URL] and
      (JsPath \ "added").write[Int] and
      (JsPath \ "deleted").write[Int] and
      (JsPath \ "comment").write[String] and
      (JsPath \ "isNew").write[Boolean] and
      (JsPath \ "isMinor").write[Boolean] and
      (JsPath \ "delta").write[Int] and
      (JsPath \ "user").write[String] and
      (JsPath \ "namespace").write[String]
    )(unlift(WikiEdit.unapply))

  private[this] val avroConverter = context.actorOf(AvroConverterActor.props(self))

  override def receive: Receive = {
    case kafkaMsg @ RawKafkaMessage(_, _) => avroConverter ! kafkaMsg

    case MessageConverted(entry) =>
      val json = Try(Json.toJson(entry))

      val jsonStr = json.map(_.toString)

      logger.debug(s"json: $jsonStr")

      jsonStr match {
        case Failure(f) => logger.error(f.getMessage)
        case _ =>
      }

      jsonStr.foreach { js =>
        context.system.eventStream.publish(ChannelMessage(entry.channel, js))
      }
  }
}

object ChannelPublisherActor {
  def props(): Props = Props(new ChannelPublisherActor)
}

