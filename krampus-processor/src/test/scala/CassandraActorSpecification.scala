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

class CassandraActorSpecification() extends TestKit(ActorSystem("CassandraActorSpecification")) with ImplicitSender
  with FunSuiteLike with WithEmbeddedCassandra with ScalaFutures with IntegrationPatience
  with Matchers with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with EmbeddedCassandraDatabaseProvider {
  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  val appConfig = new AppConfig("cassandra-processor-app")
  val config = appConfig.cassandraConfig

  implicit val db = database.Edits

  test("CassandraActor must store WikiChangeEntry in Cassandra") {
    import scala.concurrent.ExecutionContext.Implicits.global

    val cassandraActor = system.actorOf(CassandraActor.props(config))

    forAll(rawKafkaMessageGenerator) { case (_, wikiChange) =>
      val chainBefore = for {
        retrieve <- db.getById(wikiChange.id)
      } yield retrieve

      whenReady(chainBefore) { result =>
        result shouldBe None
      }

      cassandraActor ! Insert(wikiChange)
      expectMsg(Stored(wikiChange))

      val chainAfter = for {
        retrieve <- db.getById(wikiChange.id)
      } yield retrieve

      whenReady(chainAfter) { result =>
        result shouldBe defined
        result.value shouldBe wikiChange
      }
    }
  }
}