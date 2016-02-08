// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Actor}
import com.typesafe.scalalogging.LazyLogging
import krampus.avro.WikiChangeEntryAvro
import krampus.monitoring.util.RawKafkaMessage
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor extends Actor with LazyLogging {
  private[this] lazy val reader =
    new SpecificDatumReader[WikiChangeEntryAvro](WikiChangeEntryAvro.getClassSchema())

  override def receive: Receive = {
    case msg @ RawKafkaMessage(_, _) =>
      convert(msg)
      context.parent ! MessageConverted
  }

  def convert(msg: RawKafkaMessage): WikiChangeEntryAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiChangeEntryAvro = reader.read(null, decoder) //scalastyle:ignore

    logger.info(s"${wikiChangeEntryAvro.getChannel()}: ${wikiChangeEntryAvro.getPage()}")

    wikiChangeEntryAvro
  }
}

object AvroConverterActor {
  def props(): Props = Props(new AvroConverterActor)
}
