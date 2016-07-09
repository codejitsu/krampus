// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.Matchers
import krampus.entity.CommonGenerators._
import scala.concurrent.ExecutionContext.Implicits.global

class WikiUserCassandraSpecification extends EmbeddedCassandraSuite with Matchers with GeneratorDrivenPropertyChecks {
  test("WikiUser entities should be stored and retrieved") {
    forAll(wikiUserEntityGenerator) { case user =>
      val chain = for {
        store <- database.Users.store(user)
        retrieve <- database.Users.getById(user.id)
      } yield retrieve

      whenReady(chain) { result =>
        result shouldBe defined
        result.value shouldBe user.copy(name = user.name.trim.toLowerCase)
      }
    }
  }
}
