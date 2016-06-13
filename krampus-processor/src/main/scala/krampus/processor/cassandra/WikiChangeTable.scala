// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import java.net.URL

import scala.concurrent.Future
import com.websudos.phantom.dsl._
import krampus.entity.WikiChangeEntry

class Edits extends CassandraTable[ConcreteEdits, WikiChangeEntry] {
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

abstract class ConcreteEdits extends Edits with RootConnector {
  def store(user: WikiChangeEntry): Future[ResultSet] =
    insert
      .value(_.IsRobot, user.isRobot)
      .value(_.Channel, user.channel)
      .value(_.Timestamp, user.timestamp)
      .value(_.Flags, user.flags)
      .value(_.IsUnpatrolled, user.isUnpatrolled)
      .value(_.Page, user.page)
      .value(_.DiffUrl, user.diffUrl.toString)
      .value(_.Added, user.added)
      .value(_.Deleted, user.deleted)
      .value(_.Comment, user.comment)
      .value(_.IsNew, user.isNew)
      .value(_.IsMinor, user.isMinor)
      .value(_.Delta, user.delta)
      .value(_.User, user.user)
      .value(_.Namespace, user.namespace)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()

  def getById(id: UUID): Future[Option[WikiChangeEntry]] =
    select.where(_.Id eqs id).one()
}
