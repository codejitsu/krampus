// Copyright (C) 2016, codejitsu.

package krampus.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.actor.protocol.MessageConverted
import krampus.entity.CommonGenerators.rawKafkaMessageGenerator
import krampus.processor.actor.AvroConverterActor
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

class AvroConverterActorSpecification() extends TestKit(ActorSystem("AvroConverterActorSpecification")) with ImplicitSender
  with FunSuiteLike with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("AvroConverterActor must convert raw kafka messages to entities") {
    val actor = system.actorOf(AvroConverterActor.props(self))

    forAll(rawKafkaMessageGenerator) { case (rawMessage, converted) =>
      actor ! rawMessage
      expectMsg(MessageConverted(converted))
    }
  }
}
