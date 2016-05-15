// Copyright (C) 2016, codejitsu.

package krampus.processor.util

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

class AppConfig(appName: String) {
  lazy val config: Config = ConfigFactory.load()

  lazy val kafkaConfig: Config = config.getConfig(s"krampus.$appName.kafka")
  lazy val cassandraConfig = config.getConfig(s"krampus.$appName.cassandra")
  lazy val systemName: String = config.getString(s"krampus.$appName.system-name")

  lazy val topic: String = kafkaConfig.getString("topic")
}

//TODO move this class to commons
object AppConfig {
  implicit class ConfigDuration(val c: Config) extends AnyVal {
    def getMillis(path: String): FiniteDuration =
      FiniteDuration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
}

