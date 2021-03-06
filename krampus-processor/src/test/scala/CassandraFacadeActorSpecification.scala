// Copyright (C) 2017, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import krampus.actor.protocol.{Insert, Stored}
import krampus.entity.CommonGenerators._
import krampus.processor.cassandra.{EmbeddedCassandraDatabaseProvider, WithEmbeddedCassandra}
import krampus.processor.util.AppConfig
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class CassandraFacadeActorSpecification extends TestKit(ActorSystem("CassandraFacadeActorSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra with ScalaFutures with IntegrationPatience
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {
  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  val appConfig = new AppConfig("cassandra-processor-app")
  val config = appConfig.cassandraConfig

  implicit val db = database.WikiEdits

  test("CassandraFacadeActor must store WikiEdits in Cassandra") {
    import scala.concurrent.ExecutionContext.Implicits.global

    val cassandraFacadeActor = system.actorOf(CassandraFacadeActor.props(config, Option(testActor)), "cassandra-facade-actor")

    forAll(rawKafkaMessageGenerator) { case (_, wikiEdit) =>
      val chainBefore = for {
        retrieve <- db.getById(wikiEdit.id)
      } yield retrieve

      whenReady(chainBefore) { result =>
        result shouldBe None
      }

      cassandraFacadeActor ! Insert(wikiEdit)
      expectMsg(Stored(wikiEdit, Option(testActor)))

      val chainAfter = for {
        retrieve <- db.getById(wikiEdit.id)
      } yield retrieve

      whenReady(chainAfter) { result =>
        result.isDefined shouldBe true
        result.value shouldBe wikiEdit
      }
    }
  }
}