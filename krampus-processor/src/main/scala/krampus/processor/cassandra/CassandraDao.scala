// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import com.websudos.phantom.dsl._
import scala.concurrent.Future

trait CassandraDao[E] {
  def store(entity: E): Future[ResultSet]
  def getById(id: UUID): Future[Option[E]]
}
