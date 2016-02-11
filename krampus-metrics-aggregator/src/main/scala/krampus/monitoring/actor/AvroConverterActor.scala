// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Actor}
import com.typesafe.scalalogging.LazyLogging
import krampus.avro.WikiChangeEntryAvro
import krampus.entity.WikiChangeEntry
import krampus.monitoring.util.{AggregationMessage, AppConfig, RawKafkaMessage}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import krampus.monitoring.util.AppConfig.ConfigDuration

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor(config: AppConfig) extends Actor with LazyLogging {
  private[this] lazy val reader =
    new SpecificDatumReader[WikiChangeEntryAvro](WikiChangeEntryAvro.getClassSchema())

  private[this] lazy val allCounter =
    context.actorOf(CounterActor.props[AggregationMessage]("all-messages",
      config.aggregationConfig.getMillis("flush-interval-ms"), _ => true))

  override def receive: Receive = {
    case msg @ RawKafkaMessage(_, _) =>
      val entryAvro = convert(msg)
      val entry = fromAvro(entryAvro)

      allCounter ! AggregationMessage(entry)
      context.parent ! MessageConverted
  }

  def convert(msg: RawKafkaMessage): WikiChangeEntryAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiChangeEntryAvro = reader.read(null, decoder) //scalastyle:ignore

    logger.info(s"${wikiChangeEntryAvro.getChannel()}: ${wikiChangeEntryAvro.getPage()}")

    wikiChangeEntryAvro
  }

  def fromAvro(entryAvro: WikiChangeEntryAvro): WikiChangeEntry = WikiChangeEntry(entryAvro)
}

object AvroConverterActor {
  def props(config: AppConfig): Props = Props(new AvroConverterActor(config))
}
