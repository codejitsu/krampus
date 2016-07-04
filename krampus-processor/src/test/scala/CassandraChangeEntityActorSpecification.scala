// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiChangeEntry
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, EmbeddedCassandraSuite}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers}

class CassandraChangeEntityActorSpecification() extends EmbeddedCassandraSuite
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {

  val testKit = new TestKit(ActorSystem("CassandraChangeEntityActorSpecification")) with ImplicitSender

  import testKit._

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  implicit val db = database.Edits

  test("CassandraEntityActor for Change entity must receive Store(change) messages") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiChangeEntry])

    forAll(rawKafkaMessageGenerator) { case (_, wikiChange) =>
      actor ! Store(wikiChange)
      expectMsg(Stored(wikiChange))
    }
  }

  test("CassandraEntityActor for Change entity must ignore all other entities and return 'InvalidEntityType' error message") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiChangeEntry])

    val msg = wikiPageEntityGenerator.sample

    msg.foreach { case wikiPage =>
      actor ! Store(wikiPage)
      expectMsg(InvalidEntityType)
    }
  }
}