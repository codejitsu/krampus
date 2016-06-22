// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import java.util

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import krampus.entity.CommonGenerators._
import krampus.entity.WikiChangeEntry
import krampus.processor.util.AppConfig
import krampus.queue.RawKafkaMessage
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.scalatest.concurrent.Eventually
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer

class StreamProcessorActorSpecification() extends TestKit(ActorSystem("StreamProcessorActorSpecification"))
  with ImplicitSender
  with FunSuiteLike
  with Matchers
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with EmbeddedKafka
  with Eventually {

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

  implicit val embeddedKafkaConfig = EmbeddedKafkaConfig.defaultConfig.copy(customBrokerProperties = Map("zookeeper.connection.timeout.ms" -> "30000"))
  implicit override val generatorDrivenConfig = PropertyCheckConfig(maxSize = 10) // scalastyle:ignore
  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(15, Seconds)), interval = scaled(Span(100, Millis))) // scalastyle:ignore
  implicit val timeout: Timeout = Timeout(30 seconds)

  lazy val kafkaProducer = aKafkaProducer[RawKafkaMessage]

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("StreamProcessorActor onMessage function receives deserialized kafka messages") {
    withRunningKafka {
      val messages = ListBuffer.empty[WikiChangeEntry]

      def process(msg: WikiChangeEntry): Unit = messages += msg

      val cnf = config
      val actor = system.actorOf(StreamProcessorActor.props(cnf, process))
      actor ! StartStreamProcessor

      var counter = 0
      forAll(rawKafkaMessageGenerator) { case (rawMessage, _) =>
        kafkaProducer.send(new ProducerRecord(cnf.topic, rawMessage))
        counter = counter + 1
      }

      Thread.sleep(5000) // scalastyle:ignore

      eventually {
        assert(messages.size == counter)
      }

      system.stop(actor)
    }
  }

  def config: AppConfig = new AppConfig("cassandra-processor-app")
}
