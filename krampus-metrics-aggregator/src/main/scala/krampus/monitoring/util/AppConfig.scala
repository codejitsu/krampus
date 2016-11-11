// Copyright (C) 2016, codejitsu.

package krampus.monitoring.util

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

//TODO move it to commons
class AppConfig() {
  lazy val config = ConfigFactory
    .parseFile(new File(s"${sys.env.getOrElse("APP_CONF", ".")}/boot-configuration.conf"))
    .withFallback(ConfigFactory.load())

  lazy val kafkaConfig: Config = config.getConfig("krampus.metrics-aggregator-app.kafka")
  lazy val aggregationConfig = config.getConfig("krampus.metrics-aggregator-app.aggregation")
  lazy val systemName: String = config.getString("krampus.metrics-aggregator-app.system-name")
}

object AppConfig {
  implicit class ConfigDuration(val c: Config) extends AnyVal {
    def getMillis(path: String): FiniteDuration =
      FiniteDuration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
}

