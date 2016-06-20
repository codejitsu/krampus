// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import java.net.URL

import scala.concurrent.Future
import com.websudos.phantom.dsl._
import krampus.entity.WikiPage

class Pages extends CassandraTable[PagesRepository, WikiPage] {
  object Id extends UUIDColumn(this) with PartitionKey[UUID]
  object Title extends StringColumn(this)
  object Url extends StringColumn(this)

  def fromRow(row: Row): WikiPage =
    WikiPage(
      Id(row),
      Title(row),
      new URL(Url(row))
    )
}

abstract class PagesRepository extends Pages with RootConnector {
  def store(page: WikiPage): Future[ResultSet] =
    insert.value(_.Id, page.id).value(_.Title, page.title)
      .value(_.Url, page.url.toString).ifNotExists()
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()

  def getById(id: UUID): Future[Option[WikiPage]] =
    select.where(_.Id eqs id).one()
}
