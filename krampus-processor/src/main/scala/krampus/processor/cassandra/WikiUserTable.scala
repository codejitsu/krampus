// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import scala.concurrent.Future
import com.websudos.phantom.dsl._
import krampus.entity.WikiUser

class Users extends CassandraTable[UsersRepository, WikiUser] {
  object Id extends UUIDColumn(this) with PartitionKey[UUID]
  object Name extends StringColumn(this)
  object IsRobot extends BooleanColumn(this)

  def fromRow(row: Row): WikiUser =
    WikiUser(
      Id(row),
      Name(row),
      IsRobot(row)
    )
}

abstract class UsersRepository extends Users with RootConnector with CassandraDao[WikiUser] {
  def store(user: WikiUser): Future[ResultSet] =
    insert.value(_.Id, user.id).value(_.Name, user.name)
      .value(_.IsRobot, user.isRobot).ifNotExists()
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()

  def getById(id: UUID): Future[Option[WikiUser]] =
    select.where(_.Id eqs id).one()
}
