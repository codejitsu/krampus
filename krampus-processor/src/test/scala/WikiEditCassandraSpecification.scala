// Copyright (C) 2017, codejitsu.

package krampus.processor.cassandra

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.Matchers
import krampus.entity.CommonGenerators._
import org.scalatest.concurrent.IntegrationPatience

import scala.concurrent.ExecutionContext.Implicits.global

class WikiEditCassandraSpecification extends EmbeddedCassandraSuite with Matchers
  with GeneratorDrivenPropertyChecks
  with IntegrationPatience {

  test("WikiEdit entities should be stored and retrieved") {
    forAll(rawKafkaMessageGenerator) { case (_, wikiEdit) =>
      val chain = for {
        store <- database.WikiEdits.store(wikiEdit)
        retrieve <- database.WikiEdits.getById(wikiEdit.id)
      } yield retrieve

      whenReady(chain) { result =>
        result.isDefined shouldBe true
        result.value shouldBe wikiEdit
      }
    }
  }
}
