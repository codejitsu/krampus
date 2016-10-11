// Copyright (C) 2016, codejitsu.

package krampus.score.actor

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import krampus.actor.{AvroConverterActor, KafkaListenerActor, StatsD}
import krampus.actor.protocol.{InitializeQueueListener, MessageConverted, QueueListenerInitialized, StartStreamProcessor}
import krampus.entity.WikiEdit
import krampus.queue.RawKafkaMessage
import krampus.score.util.{AppConfig, MonitoringMessage}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.clustering.KMeansModel
import krampus.score.util.AppConfig._

import scala.collection.{Map, mutable}

/**
  * Root actor node.
  */
class NodeGuardianActor(config: AppConfig) extends Actor with ActorLogging {
  private[this] lazy val counters: mutable.Map[String, ActorRef] = mutable.Map.empty

  private[this] var models: Map[String, KMeansModel] = Map.empty

  private[this] val epsilon = config.scoreConfig.getDouble("error-epsilon")

  private[this] lazy val statsdConnection =
    (config.scoreConfig.getString("statsd.host"), config.scoreConfig.getInt("statsd.port"))

  private[this] val statsdGateway =
    new StatsD(context, statsdConnection._1, statsdConnection._2,
      packetBufferSize = config.scoreConfig.getInt("statsd.packet-buffer-size"))

  val conf = new SparkConf()
    .setAppName("KrampusScoreApp")
    .setMaster("local")

  private[this] val sparkContext = new SparkContext(conf)

  private[this] lazy val allCounter =
    context.actorOf(CounterActor.props[MonitoringMessage]("all-messages",
      config.scoreConfig.getMillis("flush-interval-ms"),
      _ => true, statsdGateway, None, None), "all-messages-counter")

  private[this] val avroConverter = context.actorOf(AvroConverterActor.props(self), "avro-converter")
  private[this] val kafkaListener = context.actorOf(KafkaListenerActor.props(config.kafkaConfig, processKafkaMessage),
    "kafka-listener")

  private[this] def processKafkaMessage(msg: RawKafkaMessage): Unit = avroConverter ! msg

  override def preStart(): Unit = {
    val modelPath = config.scoreConfig.getString("model-path")
    log.info(s"K-Means models path: $modelPath")

    val allModels = getListOfModelDirs(modelPath)
    log.info(s"# K-Means models files: ${allModels.size}")

    allModels.foreach(f => log.info(s"Model file: ${f.getAbsolutePath}"))

    models = allModels.foldLeft(Map.empty[String, KMeansModel]) { (map, file) =>
      val model = KMeansModel.load(sparkContext, file.getAbsolutePath)

      val channel = s"#${file.getName.split("-").last}.wikipedia"

      log.info(s"Loading k-means model for the channel '$channel'")

      map + (channel -> model)
    }

    super.preStart()
  }

  private def getListOfModelDirs(parentDir: String): List[File] = {
    val d = new File(parentDir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isDirectory).toList
    } else {
      List[File]()
    }
  }

  override def receive: Receive = {
    case StartStreamProcessor =>
      log.debug("Start initializing kafka listener.")
      kafkaListener ! InitializeQueueListener

    case QueueListenerInitialized =>
      log.info("Kafka listener initialized.")

    case MessageConverted(msg) =>
      log.debug(s"Message converted: $msg")
      onMessage(msg)
  }

  def onMessage(msg: WikiEdit): Unit = {
    val monMsg = MonitoringMessage(msg)

    val channelCounter = counters.getOrElseUpdate(msg.channel,
      context.actorOf(CounterActor.props[MonitoringMessage](msg.channel,
        config.scoreConfig.getMillis("flush-interval-ms"),
        e => e.msg.channel == msg.channel, statsdGateway, models.get(msg.channel), Option(epsilon)),
        s"${msg.channel.drop(1)}-counter"))

    allCounter ! monMsg
    channelCounter ! monMsg
  }
}

object NodeGuardianActor {
  def props(config: AppConfig): Props = Props(new NodeGuardianActor(config))
}
