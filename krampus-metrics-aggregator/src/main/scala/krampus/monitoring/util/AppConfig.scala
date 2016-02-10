// Copyright (C) 2016, codejitsu.

package krampus.monitoring.util

import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.duration.FiniteDuration

class AppConfig() {

  lazy val config: Config = ConfigFactory.load()

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

