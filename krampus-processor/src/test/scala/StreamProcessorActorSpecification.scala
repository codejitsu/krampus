// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import java.util

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiEdit
import krampus.processor.util.AppConfig
import krampus.queue.RawKafkaMessage
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serializer
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class StreamProcessorActorSpecification() extends TestKit(ActorSystem("StreamProcessorActorSpecification"))
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

  val kafkaProducer = aKafkaProducer[RawKafkaMessage]

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("StreamProcessorActor onMessage function receives deserialized kafka messages") {
    withRunningKafka {
      val messages = ListBuffer.empty[WikiEdit]

      def process(msg: WikiEdit): Unit = messages += msg

      val cnf = config
      val actor = system.actorOf(StreamProcessorActor.props(cnf, process))
      actor ! StartStreamProcessor

      var counter = 0
      val futures = ListBuffer.empty[Future[RecordMetadata]]
      forAll(rawKafkaMessageGenerator) { case (rawMessage, _) =>
        val fut = Future { kafkaProducer.send(new ProducerRecord(cnf.topic, rawMessage)).get() }

        fut.onSuccess { case succ =>
          counter = counter + 1
        }

        futures += fut
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

  def config: AppConfig = new AppConfig("cassandra-processor-app")
}
