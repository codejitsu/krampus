// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import com.websudos.phantom.connectors.ContactPoint
import com.websudos.phantom.dsl.{Database, KeySpaceDef}

object Defaults {
  val keySpaceName = "wiki"
  val local = ContactPoint.local.keySpace(keySpaceName)
  val embedded = ContactPoint.embedded.keySpace(keySpaceName)
}

abstract class CassandraDatabase(val keyspace: KeySpaceDef) extends Database(keyspace) {
  object Users extends UsersRepository with keyspace.Connector
  object Pages extends PagesRepository with keyspace.Connector
  object Edits extends EditsRepository with keyspace.Connector
}

trait CassandraDatabaseProvider {
  def database: CassandraDatabase
}

object LocalCassandraWikiDatabase extends CassandraDatabase(Defaults.local)
object EmbeddedCassandraWikiDatabase extends CassandraDatabase(Defaults.embedded)

trait ProductionCassandraDatabaseProvider extends CassandraDatabaseProvider {
  override def database: CassandraDatabase = LocalCassandraWikiDatabase
}

trait EmbeddedCassandraDatabaseProvider extends CassandraDatabaseProvider {
  override def database: CassandraDatabase = EmbeddedCassandraWikiDatabase
}

