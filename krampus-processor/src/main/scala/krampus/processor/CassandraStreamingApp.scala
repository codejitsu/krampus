// Copyright (C) 2016, codejitsu.

package krampus.processor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import com.softwaremill.react.kafka.KafkaMessages._
import com.softwaremill.react.kafka.{ConsumerProperties, PublisherWithCommitSink, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import com.websudos.phantom.dsl._
import kafka.serializer.Decoder
import krampus.avro.WikiEditAvro
import krampus.entity.WikiEdit
import krampus.processor.cassandra.ProductionCassandraDatabaseProvider
import krampus.processor.util.AppConfig
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader

import scala.concurrent.duration._
import scala.util.control.NonFatal

object CassandraStreamingApp extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Starting krampus cassandra streaming app...")

    val appConfig = new AppConfig("cassandra-processor-app")

    logger.info(appConfig.config.root().render())

    new StreamingCassandra(appConfig).start()
  }
}

class StreamingCassandra(config: AppConfig) extends LazyLogging with ProductionCassandraDatabaseProvider {
  implicit val system = ActorSystem("krampus-streaming-cassandra")
  implicit val materializer = ActorMaterializer.create(system)

  private[this] lazy val dao = database.WikiEdits
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
    .setProperty("zookeeper.connection.timeout.ms", config.kafkaConfig.getString("zookeeper-connection-timeout-ms"))
    .setProperty("zookeeper.session.timeout.ms", config.kafkaConfig.getString("zookeeper-session-timeout-ms"))

  private[this] val consumerWithOffsetSink: Option[PublisherWithCommitSink[Array[Byte]]] =
    Option(reactiveKafka.consumeWithOffsetSink(consumerProperties))

  private[this] lazy val reader =
    new SpecificDatumReader[WikiEditAvro](WikiEditAvro.getClassSchema())

  def start(): Unit = try {
    consumerWithOffsetSink.foreach { consumer =>
      logger.info("Starting the kafka listener...")

      Source.fromPublisher(consumer.publisher)
        .map(processKafkaMessage)
        .map(parseAvro)
        .via(storeCassandra)
        .to(consumer.offsetCommitSink).run()
    }
  } catch {
    case NonFatal(e) => logger.error(e.getMessage, e)
  }

  def processKafkaMessage(msg: KafkaMessage[Array[Byte]]): (RawKafkaMessage, KafkaMessage[Array[Byte]]) =
    (RawKafkaMessage(msg.key(), msg.message()), msg)

  def parseAvro(msg: (RawKafkaMessage, KafkaMessage[Array[Byte]])): (WikiEdit, KafkaMessage[Array[Byte]]) = {
    val entryAvro = deserializeMessage(msg._1)
    (fromAvro(entryAvro), msg._2)
  }

  //TODO remove duplicates
  def deserializeMessage(msg: RawKafkaMessage): WikiEditAvro = {
    val decoder = DecoderFactory.get().binaryDecoder(msg.msg, null) //scalastyle:ignore
    val wikiEditAvro = reader.read(null, decoder) //scalastyle:ignore

    logger.debug(s"${wikiEditAvro.getChannel()}: ${wikiEditAvro.getPage()}")

    wikiEditAvro
  }

  //TODO remove duplicates
  def fromAvro(entryAvro: WikiEditAvro): WikiEdit = WikiEdit(entryAvro)

  def storeCassandra: Flow[(WikiEdit, KafkaMessage[Array[Byte]]), KafkaMessage[Array[Byte]], Unit] =
    Flow[(WikiEdit, KafkaMessage[Array[Byte]])].mapAsync(1) { msg =>
      dao.store(msg._1) map { (_, msg._2) }
    }.collect {
      case (_, bytes) => bytes
    }
}
