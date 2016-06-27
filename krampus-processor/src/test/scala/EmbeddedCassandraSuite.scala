// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import krampus.processor.cassandra.EmbeddedCassandraDatabaseProvider
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class EmbeddedCassandraSuite extends Suite
  with BeforeAndAfterAll
  with ScalaFutures
  with Matchers
  with OptionValues
  with EmbeddedCassandraDatabaseProvider {

  override def beforeAll(): Unit = {
    super.beforeAll()
    // Automatically create every single table in Cassandra.

    val db = database

    implicit val session = db.session
    implicit val keySpace = db.space

    Await.result(db.autocreate.future(), 5 seconds)
  }
}

