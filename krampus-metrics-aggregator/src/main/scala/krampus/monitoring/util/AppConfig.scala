// Copyright (C) 2016, codejitsu.

package krampus.monitoring.util

import com.typesafe.config.{ConfigFactory, Config}

class AppConfig() {
  lazy val config: Config = ConfigFactory.load()
  def kafkaConfig(): Config = config.getConfig("krampus.metrics-aggregator-app.kafka")
}
