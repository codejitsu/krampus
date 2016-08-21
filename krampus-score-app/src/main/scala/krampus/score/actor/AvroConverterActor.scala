// Copyright (C) 2016, codejitsu.

package krampus.score.actor

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.avro.WikiEditAvro
import krampus.entity.WikiEdit
import krampus.score.util.{MonitoringMessage, AppConfig}
import krampus.score.util.AppConfig._
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.clustering.KMeansModel

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
    context.actorOf(CounterActor.props[MonitoringMessage]("all-messages",
      config.scoreConfig.getMillis("flush-interval-ms"),
      _ => true, statsdGateway, None, None), "all-messages-counter")

  private[this] val epsilon = config.scoreConfig.getDouble("error-epsilon")

  private[this] lazy val counters: mutable.Map[String, ActorRef] = mutable.Map.empty

  private[this] var models: Map[String, KMeansModel] = Map.empty

  val conf = new SparkConf()
    .setAppName("KrampusScoreApp")
    .setMaster("local")

  private[this] val sparkContext = new SparkContext(conf)

  private def getListOfModelDirs(parentDir: String):List[File] = {
    val d = new File(parentDir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isDirectory).toList
    } else {
      List[File]()
    }
  }

  override def preStart(): Unit = {
    val modelPath = config.scoreConfig.getString("model-path")
    logger.info(s"K-Means models path: $modelPath")

    val allModels = getListOfModelDirs(modelPath)
    logger.info(s"# K-Means models files: ${allModels.size}")

    allModels.foreach(f => logger.info(s"Model file: ${f.getAbsolutePath}"))

    models = allModels.foldLeft(Map.empty[String, KMeansModel]) { (map, file) =>
      val model = KMeansModel.load(sparkContext, file.getAbsolutePath)

      val channel = s"#${file.getName.split("-").last}.wikipedia"

      logger.info(s"Loading k-means model for the channel '$channel'")

      map + (channel -> model)
    }

    super.preStart()
  }

  override def receive: Receive = {
    case msg @ RawKafkaMessage(_, _) =>
      val entryAvro = convert(msg)
      val entry = fromAvro(entryAvro)

      val monMsg = MonitoringMessage(entry)

      val channelCounter = counters.getOrElseUpdate(entry.channel,
        context.actorOf(CounterActor.props[MonitoringMessage](entry.channel,
          config.scoreConfig.getMillis("flush-interval-ms"),
        e => e.msg.channel == entry.channel, statsdGateway, models.get(entry.channel), Option(epsilon)),
          s"${entry.channel.drop(1)}-counter"))

      allCounter ! monMsg
      channelCounter ! monMsg

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
