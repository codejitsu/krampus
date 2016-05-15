// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import krampus.entity.CommonGenerators._
import krampus.processor.util.AppConfig
import krampus.queue.RawKafkaMessage
import net.manub.embeddedkafka.EmbeddedKafka

class KafkaListenerActorSpecification() extends TestKit(ActorSystem("KafkaListenerActorSpecification"))
  with ImplicitSender
  with FunSuiteLike
  with Matchers
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with EmbeddedKafka {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("KafkaListenerActor must forward raw kafka messages to avro converter actor") {
    def process(msg: RawKafkaMessage): Unit = ()

    val actor = system.actorOf(KafkaListenerActor.props(config, process))

    withRunningKafka {
/*      forAll(rawKafkaMessageGenerator) { case (rawMessage, converted) =>
        actor ! rawMessage
        expectMsg(MessageConverted(converted))
      } */
    }
  }

  def config: AppConfig = new AppConfig("cassandra-processor-app")
}