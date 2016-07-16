// Copyright (C) 2016, codejitsu.

package krampus.processor.cassandra

import com.websudos.phantom.connectors.{ContactPoint, ContactPoints}
import com.websudos.phantom.dsl.{Database, KeySpaceDef}
import krampus.processor.util.AppConfig

object Defaults {
  val keySpaceName = "wiki"
  lazy val conf = new AppConfig("cassandra-processor-app")

  lazy val local = ContactPoints(conf.cassandraConfig.getString("nodes").split(","),
    conf.cassandraConfig.getInt("port")).keySpace(keySpaceName)
  lazy val embedded = ContactPoint.embedded.keySpace(keySpaceName)
}

abstract class CassandraDatabase(val keyspace: KeySpaceDef) extends Database(keyspace) {
  object WikiEdits extends WikiEditsRepository with keyspace.Connector
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

