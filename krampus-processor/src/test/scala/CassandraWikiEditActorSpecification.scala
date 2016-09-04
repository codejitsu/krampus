// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.actor.protocol.{Store, Stored}
import krampus.entity.CommonGenerators._
import krampus.entity.WikiEdit
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, WithEmbeddedCassandra}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

class CassandraWikiEditActorSpecification extends TestKit(ActorSystem("CassandraWikiEditActorSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {
  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  implicit val db = database.WikiEdits

  test("CassandraEntityActor for WikiEdit entity must receive Store(edit) messages") {
    val actor = system.actorOf(CassandraEntityActor.props[WikiEdit])

    forAll(rawKafkaMessageGenerator) { case (_, wikiEdit) =>
      actor ! Store(wikiEdit, testActor, None)
      expectMsg(Stored(wikiEdit, None))
    }
  }
}