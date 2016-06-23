// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiUser

class CassandraUserEntityActorSpecification() extends TestKit(ActorSystem("CassandraUserEntityActorSpecification")) with ImplicitSender
  with FunSuiteLike with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  test("CassandraEntityActor for User entity must receive Store(user) messages") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiUser])

    forAll(wikiUserEntityGenerator) { case wikiUser =>
      actor ! Store(wikiUser)
      expectMsg(Stored(wikiUser))
    }
  }

  test("CassandraEntityActor for User entity must ignore all other entities") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiUser])

    val msg = rawKafkaMessageGenerator.sample

    msg.foreach { case (_, wikiEntry) =>
      actor ! Store(wikiEntry)
      expectNoMsg()
    }
  }
}