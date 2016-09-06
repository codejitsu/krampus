// Copyright (C) 2016, codejitsu.

package krampus.score.actor

import java.io.File

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Decoder
import krampus.actor.AvroConverterActor
import krampus.actor.protocol.MessageConverted
import krampus.score.util.{AppConfig, MonitoringMessage}
import krampus.queue.RawKafkaMessage
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.clustering.KMeansModel
import krampus.score.util.AppConfig._

import scala.collection.{Map, mutable}
import scala.concurrent.duration._

/**
  * Actor to read all kafka events and propagate them to aggregate actors.
  */
class KafkaListenerActor(config: AppConfig) extends Actor with LazyLogging {
  import context.system
  implicit val materializer = ActorMaterializer.create(context.system)

  private[this] var consumerWithOffsetSink: Option[PublisherWithCommitSink[Array[Byte]]] = None

  private[this] lazy val reactiveKafka: ReactiveKafka = new ReactiveKafka()

  // consumer
  private[this] lazy val consumerProperties = ConsumerProperties(
    brokerList = config.kafkaConfig.getString("broker-list"),
    zooKeeperHost = config.kafkaConfig.getString("zookeeper-host"),
    topic = config.kafkaConfig.getString("topic"),
    groupId = config.kafkaConfig.getString("group-id"),
    decoder = new Decoder[Array[Byte]] {
      override def fromBytes(bytes: Array[Byte]): Array[Byte] = bytes
    }
  ).commitInterval(1200 milliseconds)

  private[this] val avroConverter = context.actorOf(AvroConverterActor.props(self), "avro-converter")

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

  private def getListOfModelDirs(parentDir: String): List[File] = {
    val d = new File(parentDir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isDirectory).toList
    } else {
      List[File]()
    }
  }

  override def receive: Receive = {
    case InitializeListener =>
      initListener()
      context.parent ! ListenerInitialized

    case MessageConverted(entry) =>
      val monMsg = MonitoringMessage(entry)

      val channelCounter = counters.getOrElseUpdate(entry.channel,
        context.actorOf(CounterActor.props[MonitoringMessage](entry.channel,
          config.scoreConfig.getMillis("flush-interval-ms"),
          e => e.msg.channel == entry.channel, statsdGateway, models.get(entry.channel), Option(epsilon)),
          s"${entry.channel.drop(1)}-counter"))

      allCounter ! monMsg
      channelCounter ! monMsg


    case Terminated(_) =>
      logger.error("The consumer has been terminated, restarting the whole stream...")
      initListener()

    case msg =>
      logger.error(s"Unexpected message: $msg")
  }

  def initListener(): Unit = {
    consumerWithOffsetSink = Option(reactiveKafka.consumeWithOffsetSink(consumerProperties))

    consumerWithOffsetSink.foreach { consumer =>
      logger.info("Starting the kafka listener...")

      context.watch(consumer.publisherActor)

      Source.fromPublisher(consumer.publisher)
        .map(processMessage)
        .to(consumer.offsetCommitSink).run()
    }
  }

  private def processMessage(msg: KafkaMessage[Array[Byte]]) = {
    avroConverter ! RawKafkaMessage(msg.key(), msg.message())

    msg
  }

  override def postStop(): Unit = {
    consumerWithOffsetSink.foreach { consumer =>
      consumer.cancel()
    }

    super.postStop()
  }
}

object KafkaListenerActor {
  def props(config: AppConfig): Props = Props(new KafkaListenerActor(config))
}
