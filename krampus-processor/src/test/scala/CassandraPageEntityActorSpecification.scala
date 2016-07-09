// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiPage
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, WithEmbeddedCassandra}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

class CassandraPageEntityActorSpecification() extends TestKit(ActorSystem("CassandraPageEntityActorSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {

  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  implicit val db = database.Pages

  test("CassandraEntityActor for Page entity must receive Store(page) messages") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiPage])

    forAll(wikiPageEntityGenerator) { case wikiPage =>
      actor ! Store(wikiPage, testActor)
      expectMsg(Stored(wikiPage))
    }
  }

  test("CassandraEntityActor for Page entity must ignore all other entities and return 'InvalidEntityType' error message") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiPage])

    val msg = rawKafkaMessageGenerator.sample

    msg.foreach { case (_, wikiEntry) =>
      actor ! Store(wikiEntry, testActor)
      expectMsg(InvalidEntityType)
    }
  }
}