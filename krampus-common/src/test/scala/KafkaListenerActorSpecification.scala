// Copyright (C) 2016, codejitsu.

package krampus.actor

import java.util

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.{Config, ConfigFactory}
import krampus.actor.protocol.{InitializeQueueListener, QueueListenerInitialized}
import krampus.entity.CommonGenerators._
import krampus.queue.RawKafkaMessage
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serializer
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

class KafkaListenerActorSpecification() extends TestKit(ActorSystem("KafkaListenerActorSpecification"))
  with ImplicitSender
  with FunSuiteLike
  with Matchers
  with ScalaFutures
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with EmbeddedKafka
  with EmbeddedGeneratorDrivenKafkaConfig
  with Eventually
  with IntegrationPatience {

  implicit val serializer = new Serializer[RawKafkaMessage] {
    private var isKey: Boolean = false

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
      this.isKey = isKey
    }

    override def serialize(topic: String, data: RawKafkaMessage): Array[Byte] = if (isKey) {
      data.key
    } else {
      data.msg
    }

    override def close(): Unit = ()
  }

  lazy val kafkaProducer = aKafkaProducer[RawKafkaMessage]
  implicit val kafkaConf = embeddedKafkaConfig

  private val kafkaPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = scaled(Span(240, Seconds)), //scalastyle:ignore
      interval = scaled(Span(600, Millis)) //scalastyle:ignore
    )

  val actorTimeout = 300 seconds

  implicit override val patienceConfig: PatienceConfig = kafkaPatienceConfig

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("KafkaListenerActor must read and process messages from kafka") {
    withRunningKafka {
      val messages = ListBuffer.empty[RawKafkaMessage]

      def process(msg: RawKafkaMessage): Unit = messages += msg
      val cnf = config.getConfig("krampus.app.kafka")

      val actor = system.actorOf(KafkaListenerActor.props(cnf, process))
      actor ! InitializeQueueListener

      expectMsg(actorTimeout, QueueListenerInitialized)

      val futures = ListBuffer.empty[Future[RecordMetadata]]
      forAll(rawKafkaMessageGenerator) { case (rawMessage, _) =>
        futures += Future { kafkaProducer.send(new ProducerRecord(cnf.getString("topic"), rawMessage)).get() }
      }

      whenReady(Future.sequence(futures)) { case res =>
        eventually {
          messages.size > 0 shouldBe true
          messages.size shouldBe futures.size
        }
      }

      system.stop(actor)
    }
  }

  def config: Config = ConfigFactory.load()
}