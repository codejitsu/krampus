// Copyright (C) 2016, codejitsu.

package actors

import java.net.URL
import java.util.UUID

import actors.Messages.{ChannelMessage, KafkaRawDataMessage}
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.avro.WikiChangeEntryAvro
import krampus.entity.WikiChangeEntry
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.AppConfig

import scala.util.{Failure, Try}

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor(config: AppConfig) extends Actor with LazyLogging {
  implicit val uuidWrites = new Writes[UUID] {
    def writes(uuid: UUID) = Json.toJson(uuid.toString)
  }

  implicit val urlWrites = new Writes[URL] {
    def writes(url: URL) = Json.toJson(url.toString)
  }

  implicit val wikiChangeEntryWrites: Writes[WikiChangeEntry] = (
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
  )(unlift(WikiChangeEntry.unapply))

  private[this] lazy val reader =
    new SpecificDatumReader[WikiChangeEntryAvro](classOf[WikiChangeEntryAvro])

  override def receive: Receive = {
    case kafkaData @ KafkaRawDataMessage(_) =>
      val entryAvro = readAvro(kafkaData)
      val entry = convertFromAvro(entryAvro)

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

    case msg => logger.error(s"Unexpected message '$msg'")
  }

  private def readAvro(msg: KafkaRawDataMessage): WikiChangeEntryAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.data, null) //scalastyle:ignore
    val wikiChangeEntryAvro = reader.read(null, decoder) //scalastyle:ignore

    logger.debug(s"${wikiChangeEntryAvro.getChannel()}: ${wikiChangeEntryAvro.getPage()}")

    wikiChangeEntryAvro
  }

  private def convertFromAvro(entryAvro: WikiChangeEntryAvro): WikiChangeEntry = WikiChangeEntry(entryAvro)
}

object AvroConverterActor {
  def props(config: AppConfig): Props = Props(new AvroConverterActor(config))
}
