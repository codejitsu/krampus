// Copyright (C) 2016, codejitsu.

package controllers

import java.net.URL

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.avro.WikiChangeEntryAvro
import krampus.entity.WikiChangeEntry
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.joda.time.DateTime
import utils.AppConfig
import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class KMessage(key: Array[Byte], msg: Array[Byte])

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor(config: AppConfig) extends Actor with LazyLogging {
  implicit val urlWrites = new Writes[URL] {
    def writes(url: URL) = Json.toJson(url.toString)
  }

  implicit val wikiChangeEntryWrites: Writes[WikiChangeEntry] = (
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
    case msg @ KMessage(_, _) =>

      val entryAvro = convert(msg)
      val entry = fromAvro(entryAvro)

      val json = Json.toJson(entry)

      KafkaActor.subs().foreach { to =>
        val jsonStr = json.toString()

        logger.debug(s"json: $jsonStr")
        to ! json
      }
  }

  def convert(msg: KMessage): WikiChangeEntryAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiChangeEntryAvro = reader.read(null, decoder) //scalastyle:ignore

    logger.debug(s"${wikiChangeEntryAvro.getChannel()}: ${wikiChangeEntryAvro.getPage()}")

    wikiChangeEntryAvro
  }

  def fromAvro(entryAvro: WikiChangeEntryAvro): WikiChangeEntry = WikiChangeEntry(entryAvro)
}

object AvroConverterActor {
  def props(config: AppConfig): Props = Props(new AvroConverterActor(config))
}
