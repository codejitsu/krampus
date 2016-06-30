// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.Matchers
import krampus.entity.CommonGenerators._
import scala.concurrent.ExecutionContext.Implicits.global

class WikiChangeEntryCassandraSpecification extends EmbeddedCassandraSuite with Matchers with GeneratorDrivenPropertyChecks {
  test("WikiChangeEntry entities should be stored and retrieved") {
    forAll(rawKafkaMessageGenerator) { case (_, wikiChange) =>
      val chain = for {
        store <- database.Edits.store(wikiChange)
        retrieve <- database.Edits.getById(wikiChange.id)
      } yield retrieve

      whenReady(chain) { result =>
        result shouldBe defined
        result.value shouldBe wikiChange
      }
    }
  }
}
