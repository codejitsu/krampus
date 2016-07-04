// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiPage
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, EmbeddedCassandraSuite}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers}

class CassandraPageEntityActorSpecification() extends EmbeddedCassandraSuite
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {

  val testKit = new TestKit(ActorSystem("CassandraPageEntityActorSpecification")) with ImplicitSender

  import testKit._

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  implicit val db = database.Pages

  test("CassandraEntityActor for Page entity must receive Store(page) messages") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiPage])

    forAll(wikiPageEntityGenerator) { case wikiPage =>
      actor ! Store(wikiPage)
      expectMsg(Stored(wikiPage))
    }
  }

  test("CassandraEntityActor for Page entity must ignore all other entities and return 'InvalidEntityType' error message") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiPage])

    val msg = rawKafkaMessageGenerator.sample

    msg.foreach { case (_, wikiEntry) =>
      actor ! Store(wikiEntry)
      expectMsg(InvalidEntityType)
    }
  }
}