// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import java.util

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import krampus.entity.CommonGenerators._
import krampus.processor.util.AppConfig
import krampus.queue.RawKafkaMessage
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.duration._

class KafkaListenerActorSpecification() extends TestKit(ActorSystem("KafkaListenerActorSpecification"))
  with ImplicitSender
  with FunSuiteLike
  with Matchers
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with EmbeddedKafka {

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

  implicit override val generatorDrivenConfig = PropertyCheckConfig(maxSize = 10)
  implicit val timeout: Timeout = Timeout(30 seconds)

  lazy val kafkaProducer = aKafkaProducer[RawKafkaMessage]

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("KafkaListenerActor must forward raw kafka messages to avro converter actor") {
    def process(msg: RawKafkaMessage): Unit = ()
    val cnf = config

    val actor = system.actorOf(KafkaListenerActor.props(cnf, process))
    actor ! InitializeListener

    withRunningKafka {
      forAll(rawKafkaMessageGenerator) { case (rawMessage, converted) =>
        kafkaProducer.send(new ProducerRecord(cnf.topic, rawMessage))
      }
    }

    system.stop(actor)
  }

  def config: AppConfig = new AppConfig("cassandra-processor-app")
}