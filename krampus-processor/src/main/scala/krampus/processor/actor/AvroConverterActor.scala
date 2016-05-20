// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import krampus.avro.WikiChangeEntryAvro
import krampus.entity.WikiChangeEntry
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor(recipient: ActorRef) extends Actor with ActorLogging {
  private[this] lazy val reader =
    new SpecificDatumReader[WikiChangeEntryAvro](WikiChangeEntryAvro.getClassSchema())

  override def receive: Receive = {
    case msg @ RawKafkaMessage(_, _) =>
      val entryAvro = deserializeMessage(msg)
      val entry = fromAvro(entryAvro)

      recipient ! MessageConverted(entry)
  }

  def deserializeMessage(msg: RawKafkaMessage): WikiChangeEntryAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiChangeEntryAvro = reader.read(null, decoder) //scalastyle:ignore

    log.debug(s"${wikiChangeEntryAvro.getChannel()}: ${wikiChangeEntryAvro.getPage()}")

    wikiChangeEntryAvro
  }

  def fromAvro(entryAvro: WikiChangeEntryAvro): WikiChangeEntry = WikiChangeEntry(entryAvro)
}

object AvroConverterActor {
  def props(recipient: ActorRef): Props = Props(new AvroConverterActor(recipient))
}
