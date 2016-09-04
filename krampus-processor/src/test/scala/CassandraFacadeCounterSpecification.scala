// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.actor.protocol.{CountInserted, GetCountInserted, Insert, Stored}
import krampus.entity.CommonGenerators._
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, WithEmbeddedCassandra}
import krampus.processor.util.AppConfig
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class CassandraFacadeCounterSpecification extends TestKit(ActorSystem("CassandraFacadeCounterSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra with ScalaFutures with IntegrationPatience
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {
  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  val appConfig = new AppConfig("cassandra-processor-app")
  val config = appConfig.cassandraConfig

  implicit val db = database.WikiEdits

  test("CassandraFacadeActor must count stored entities") {
    val cassandraFacadeActor = system.actorOf(CassandraFacadeActor.props(config, Option(testActor)), "cassandra-facade-actor")

    cassandraFacadeActor ! GetCountInserted
    expectMsg(CountInserted(0))

    var counter = 0
    forAll(rawKafkaMessageGenerator) { case (_, wikiEdit) =>
      cassandraFacadeActor ! Insert(wikiEdit)
      expectMsg(Stored(wikiEdit, Option(testActor)))

      counter = counter + 1

      cassandraFacadeActor ! GetCountInserted
      expectMsg(CountInserted(counter))
    }

    system.stop(cassandraFacadeActor)
  }
}