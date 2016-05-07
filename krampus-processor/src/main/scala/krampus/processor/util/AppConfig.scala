// Copyright (C) 2016, codejitsu.

package krampus.processor.util

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

class AppConfig() {

  lazy val config: Config = ConfigFactory.load()

  lazy val kafkaConfig: Config = config.getConfig("krampus.cassandra-processor-app.kafka")
  lazy val cassandraConfig = config.getConfig("krampus.cassandra-processor-app.cassandra")
  lazy val systemName: String = config.getString("krampus.metrics-aggregator-app.system-name")
}

//TODO move this class to commons
object AppConfig {
  implicit class ConfigDuration(val c: Config) extends AnyVal {
    def getMillis(path: String): FiniteDuration =
      FiniteDuration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
}

