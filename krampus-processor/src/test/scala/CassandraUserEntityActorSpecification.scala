// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiUser
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, WithEmbeddedCassandra}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

class CassandraUserEntityActorSpecification() extends TestKit(ActorSystem("CassandraUserEntityActorSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {

  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  implicit val db = database.Users

  test("CassandraEntityActor for User entity must receive Store(user) messages") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiUser])

    forAll(wikiUserEntityGenerator) { case wikiUser =>
      actor ! Store(wikiUser)
      expectMsg(Stored(wikiUser))
    }
  }

  test("CassandraEntityActor for User entity must ignore all other entities and return 'InvalidEntityType' error message") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiUser])

    val msg = rawKafkaMessageGenerator.sample

    msg.foreach { case (_, wikiEntry) =>
      actor ! Store(wikiEntry)
      expectMsg(InvalidEntityType)
    }
  }
}