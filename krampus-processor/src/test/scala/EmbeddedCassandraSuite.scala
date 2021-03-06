// Copyright (C) 2017, codejitsu.

package krampus.processor.cassandra

import org.scalatest.concurrent.ScalaFutures
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class EmbeddedCassandraSuite extends FunSuite with WithEmbeddedCassandra

trait WithEmbeddedCassandra extends BeforeAndAfterAll
  with ScalaFutures
  with Matchers
  with OptionValues
  with EmbeddedCassandraDatabaseProvider {
  self: Suite =>

  override def beforeAll(): Unit = {
    super.beforeAll()
    // Automatically create every single table in Cassandra.

    val db = database

    implicit val session = db.session
    implicit val keySpace = db.space

    Await.result(db.autocreate.future(), 5 seconds)
  }
}