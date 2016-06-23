// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import com.websudos.phantom.connectors.ContactPoint
import com.websudos.phantom.dsl.{Database, KeySpaceDef}

object Defaults {
  val keySpaceName = "wiki"
  val connector = ContactPoint.local.keySpace(keySpaceName)
}

class CassandraDb(val keyspace: KeySpaceDef) extends Database(keyspace) {
  object Users extends UsersRepository with keyspace.Connector
  object Pages extends PagesRepository with keyspace.Connector
  object Edits extends EditsRepository with keyspace.Connector
}

object WikiDb extends CassandraDb(Defaults.connector)
