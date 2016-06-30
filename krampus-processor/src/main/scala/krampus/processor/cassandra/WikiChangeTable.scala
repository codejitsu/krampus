// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import java.net.URL

import scala.concurrent.Future
import com.websudos.phantom.dsl._
import krampus.entity.WikiChangeEntry

class Edits extends CassandraTable[EditsRepository, WikiChangeEntry] {
  object Id extends UUIDColumn(this) with PartitionKey[UUID]
  object IsRobot extends BooleanColumn(this)
  object Channel extends StringColumn(this)
  object Timestamp extends DateTimeColumn(this)

  object Flags extends ListColumn[String](this)

  object IsUnpatrolled extends BooleanColumn(this)
  object Page extends StringColumn(this)
  object DiffUrl extends StringColumn(this)
  object Added extends IntColumn(this)
  object Deleted extends IntColumn(this)
  object Comment extends StringColumn(this)
  object IsNew extends BooleanColumn(this)
  object IsMinor extends BooleanColumn(this)
  object Delta extends IntColumn(this)
  object User extends StringColumn(this) // should be UUID key
  object Namespace extends StringColumn(this)


  def fromRow(row: Row): WikiChangeEntry =
    WikiChangeEntry(
      Id(row),
      IsRobot(row),
      Channel(row),
      Timestamp(row),
      Flags(row),
      IsUnpatrolled(row),
      Page(row),
      new URL(DiffUrl(row)),
      Added(row),
      Deleted(row),
      Comment(row),
      IsNew(row),
      IsMinor(row),
      Delta(row),
      User(row),
      Namespace(row)
    )
}

abstract class EditsRepository extends Edits with RootConnector {
  def store(edit: WikiChangeEntry): Future[ResultSet] =
    insert
      .value(_.Id, edit.id)
      .value(_.IsRobot, edit.isRobot)
      .value(_.Channel, edit.channel)
      .value(_.Timestamp, edit.timestamp)
      .value(_.Flags, edit.flags)
      .value(_.IsUnpatrolled, edit.isUnpatrolled)
      .value(_.Page, edit.page)
      .value(_.DiffUrl, edit.diffUrl.toString)
      .value(_.Added, edit.added)
      .value(_.Deleted, edit.deleted)
      .value(_.Comment, edit.comment)
      .value(_.IsNew, edit.isNew)
      .value(_.IsMinor, edit.isMinor)
      .value(_.Delta, edit.delta)
      .value(_.User, edit.user)
      .value(_.Namespace, edit.namespace)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()

  def getById(id: UUID): Future[Option[WikiChangeEntry]] =
    select.where(_.Id eqs id).one()
}
