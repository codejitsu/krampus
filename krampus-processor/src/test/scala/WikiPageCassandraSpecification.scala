// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.Matchers
import krampus.entity.CommonGenerators._
import scala.concurrent.ExecutionContext.Implicits.global

class WikiPageCassandraSpecification extends EmbeddedCassandraSuite with Matchers with GeneratorDrivenPropertyChecks {
  test("WikiPage entities should be stored and retrieved") {
    forAll(wikiPageEntityGenerator) { case page =>
      val chain = for {
        store <- database.Pages.store(page)
        retrieve <- database.Pages.getById(page.id)
      } yield retrieve

      whenReady(chain) { result =>
        result shouldBe defined
        result.value shouldBe page
      }
    }
  }
}
