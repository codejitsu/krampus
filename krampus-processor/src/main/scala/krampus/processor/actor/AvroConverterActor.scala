// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import krampus.avro.WikiEditAvro
import krampus.entity.WikiEdit
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor(recipient: ActorRef) extends Actor with ActorLogging {
  private[this] lazy val reader =
    new SpecificDatumReader[WikiEditAvro](WikiEditAvro.getClassSchema())

  override def receive: Receive = {
    case msg @ RawKafkaMessage(_, _) =>
      val entryAvro = deserializeMessage(msg)
      val entry = fromAvro(entryAvro)

      recipient ! MessageConverted(entry)
  }

  def deserializeMessage(msg: RawKafkaMessage): WikiEditAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiEditAvro = reader.read(null, decoder) //scalastyle:ignore

    log.debug(s"${wikiEditAvro.getChannel()}: ${wikiEditAvro.getPage()}")

    wikiEditAvro
  }

  def fromAvro(entryAvro: WikiEditAvro): WikiEdit = WikiEdit(entryAvro)
}

object AvroConverterActor {
  def props(recipient: ActorRef): Props = Props(new AvroConverterActor(recipient))
}
