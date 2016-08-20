// Copyright (C) 2016, codejitsu.

package krampus.score.actor

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.avro.WikiEditAvro
import krampus.entity.WikiEdit
import krampus.score.util.{AggregationMessage, AppConfig}
import krampus.score.util.AppConfig._
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader

/**
  * Bytes to Avro converter actor.
  */
class AvroConverterActor(config: AppConfig) extends Actor with LazyLogging {
  import scala.collection._

  private[this] lazy val reader =
    new SpecificDatumReader[WikiEditAvro](WikiEditAvro.getClassSchema())

  private[this] lazy val statsdConnection =
    (config.scoreConfig.getString("statsd.host"), config.scoreConfig.getInt("statsd.port"))

  private[this] val statsdGateway =
    new StatsD(context, statsdConnection._1, statsdConnection._2,
      packetBufferSize = config.scoreConfig.getInt("statsd.packet-buffer-size"))

  private[this] lazy val allCounter =
    context.actorOf(CounterActor.props[AggregationMessage]("all-messages",
      config.scoreConfig.getMillis("flush-interval-ms"),
      _ => true, statsdGateway), "all-messages-counter")

  private[this] lazy val counters: mutable.Map[String, ActorRef] = mutable.Map.empty

  override def receive: Receive = {
    case msg @ RawKafkaMessage(_, _) =>
      val entryAvro = convert(msg)
      val entry = fromAvro(entryAvro)

      val aggMsg = AggregationMessage(entry)

      val channelCounter = counters.getOrElseUpdate(entry.channel,
        context.actorOf(CounterActor.props[AggregationMessage](entry.channel,
          config.scoreConfig.getMillis("flush-interval-ms"),
        e => e.msg.channel == entry.channel, statsdGateway), s"${entry.channel.drop(1)}-counter"))

      allCounter ! aggMsg
      channelCounter ! aggMsg

      context.parent ! MessageConverted
  }

  def convert(msg: RawKafkaMessage): WikiEditAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiEditAvro = reader.read(null, decoder) //scalastyle:ignore

    logger.debug(s"${wikiEditAvro.getChannel()}: ${wikiEditAvro.getPage()}")

    wikiEditAvro
  }

  def fromAvro(entryAvro: WikiEditAvro): WikiEdit = WikiEdit(entryAvro)
}

object AvroConverterActor {
  def props(config: AppConfig): Props = Props(new AvroConverterActor(config))
}
