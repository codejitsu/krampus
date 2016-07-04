// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiUser
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, EmbeddedCassandraSuite}

class CassandraUserEntityActorSpecification() extends EmbeddedCassandraSuite
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {

  val testKit = new TestKit(ActorSystem("CassandraUserEntityActorSpecification")) with ImplicitSender

  import testKit._

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

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