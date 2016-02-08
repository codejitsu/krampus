// Copyright (C) 2016, codejitsu.

package krampus.monitoring.util

import com.typesafe.config.{ConfigFactory, Config}

class AppConfig() {
  lazy val config: Config = ConfigFactory.load()

  lazy val kafkaConfig: Config = config.getConfig("krampus.metrics-aggregator-app.kafka")
  lazy val systemName: String = config.getString("krampus.metrics-aggregator-app.system-name")
}
