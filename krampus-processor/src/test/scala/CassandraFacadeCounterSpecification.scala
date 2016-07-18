// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
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
    val cassandraFacadeActor = system.actorOf(CassandraFacadeActor.props(config), "cassandra-facade-actor")

    cassandraFacadeActor ! GetCountInserted
    expectMsg(CountInserted(0))

    val msg = rawKafkaMessageGenerator.sample

    msg.foreach { case (_, wikiEdit) =>
      cassandraFacadeActor ! Insert(wikiEdit)
      expectMsg(Stored(wikiEdit, testActor))
    }

    cassandraFacadeActor ! GetCountInserted
    expectMsg(CountInserted(1))

    system.stop(cassandraFacadeActor)
  }
}