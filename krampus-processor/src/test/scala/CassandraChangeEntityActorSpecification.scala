// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiChangeEntry
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, WithEmbeddedCassandra}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

class CassandraChangeEntityActorSpecification() extends TestKit(ActorSystem("CassandraChangeEntityActorSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {
  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

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