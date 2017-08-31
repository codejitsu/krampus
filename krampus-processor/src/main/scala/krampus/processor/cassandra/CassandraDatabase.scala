// Copyright (C) 2017, codejitsu.

package krampus.processor.cassandra

import com.datastax.driver.core.{HostDistance, PoolingOptions, SocketOptions}
import com.websudos.phantom.connectors.{ContactPoint, ContactPoints}
import com.websudos.phantom.dsl.{Database, KeySpaceDef}
import krampus.processor.util.AppConfig

object Defaults {
  val keySpaceName = "wiki"
  lazy val conf = new AppConfig("cassandra-processor-app")

  lazy val local = ContactPoints(conf.cassandraConfig.getString("nodes").split(","),
    conf.cassandraConfig.getInt("port")).withClusterBuilder(_.withPoolingOptions(
    new PoolingOptions().setCoreConnectionsPerHost(HostDistance.LOCAL, 4) // scalastyle:ignore
      .setMaxConnectionsPerHost(HostDistance.LOCAL, 10) // scalastyle:ignore
      .setCoreConnectionsPerHost(HostDistance.REMOTE, 2) // scalastyle:ignore
      .setMaxConnectionsPerHost(HostDistance.REMOTE, 4) // scalastyle:ignore
      .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768) // scalastyle:ignore
      .setMaxRequestsPerConnection(HostDistance.REMOTE, 2000) // scalastyle:ignore
      .setPoolTimeoutMillis(30000) // scalastyle:ignore
    ).withSocketOptions(new SocketOptions().setConnectTimeoutMillis(30000))).keySpace(keySpaceName) // scalastyle:ignore
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

