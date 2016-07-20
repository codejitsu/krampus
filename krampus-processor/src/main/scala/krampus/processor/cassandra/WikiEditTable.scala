// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import java.net.URL
import java.util.UUID

import scala.concurrent.Future
import com.websudos.phantom.dsl._
import krampus.entity.WikiEdit

class WikiEdits extends CassandraTable[WikiEditsRepository, WikiEdit] {
  object Year extends IntColumn(this) with PartitionKey[Int]  { override lazy val name = "year" }
  object Month extends IntColumn(this) with PartitionKey[Int] { override lazy val name = "month" }
  object Id extends UUIDColumn(this) with PrimaryKey[UUID] with ClusteringOrder[UUID] with Ascending
                                                              { override lazy val name = "id" }
  object Day extends IntColumn(this) with PrimaryKey[Int] with ClusteringOrder[Int] with Ascending
                                                              { override lazy val name = "day" }
  object Hour extends IntColumn(this)                         { override lazy val name = "hour" }
  object Minute extends IntColumn(this)                       { override lazy val name = "minute" }

  object Weekday extends IntColumn(this)                      { override lazy val name = "weekday" }
  object Time extends IntColumn(this) with PrimaryKey[Int] with ClusteringOrder[Int] with Ascending
                                                              { override lazy val name = "time" }
  object DayMinute extends IntColumn(this)                    { override lazy val name = "day_minute" }
  object Timestamp extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Ascending
                                                              { override lazy val name = "ts" }
  object Channel extends StringColumn(this)                   { override lazy val name = "channel" }
  object Flags extends ListColumn[String](this)               { override lazy val name = "flags" }
  object DiffUrl extends StringColumn(this)                   { override lazy val name = "diff_url" }
  object Added extends IntColumn(this)                        { override lazy val name = "added" }
  object Deleted extends IntColumn(this)                      { override lazy val name = "deleted" }
  object Comment extends StringColumn(this)                   { override lazy val name = "comment" }
  object IsUnpatrolled extends BooleanColumn(this)            { override lazy val name = "is_unpatrolled" }
  object IsNew extends BooleanColumn(this)                    { override lazy val name = "is_new" }
  object IsMinor extends BooleanColumn(this)                  { override lazy val name = "is_minor" }
  object IsRobot extends BooleanColumn(this)                  { override lazy val name = "is_robot" }
  object Delta extends IntColumn(this)                        { override lazy val name = "delta" }
  object Namespace extends StringColumn(this)                 { override lazy val name = "namespace" }
  object User extends StringColumn(this) with PrimaryKey[String] with ClusteringOrder[String] with Ascending
                                                              { override lazy val name = "user" }
  object Page extends StringColumn(this)                      { override lazy val name = "page" }

  def fromRow(row: Row): WikiEdit =
    WikiEdit(
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

  override def tableName: String = "edits"
}

abstract class WikiEditsRepository extends WikiEdits with RootConnector with CassandraDao[WikiEdit] {
  def store(edit: WikiEdit): Future[ResultSet] =
    insert
      .value(_.Id, edit.id)
      .value(_.Year, edit.timestamp.year().get())
      .value(_.Month, edit.timestamp.monthOfYear().get())
      .value(_.Day, edit.timestamp.dayOfMonth().get())
      .value(_.Hour, edit.timestamp.hourOfDay().get())
      .value(_.Minute, edit.timestamp.minuteOfHour().get())
      .value(_.Weekday, edit.timestamp.dayOfWeek().get())
      .value(_.Time, edit.timestamp.secondOfDay().get())
      .value(_.DayMinute, edit.timestamp.minuteOfDay().get())
      .value(_.Timestamp, edit.timestamp)
      .value(_.Channel, edit.channel)
      .value(_.Flags, edit.flags)
      .value(_.DiffUrl, edit.diffUrl.toString)
      .value(_.Added, edit.added)
      .value(_.Deleted, edit.deleted)
      .value(_.Comment, edit.comment)
      .value(_.IsUnpatrolled, edit.isUnpatrolled)
      .value(_.IsNew, edit.isNew)
      .value(_.IsMinor, edit.isMinor)
      .value(_.IsRobot, edit.isRobot)
      .value(_.Delta, edit.delta)
      .value(_.Namespace, edit.namespace)
      .value(_.User, edit.user)
      .value(_.Page, edit.page)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()

  def getById(id: UUID): Future[Option[WikiEdit]] =
    select.where(_.Id eqs id).allowFiltering().one()
}
