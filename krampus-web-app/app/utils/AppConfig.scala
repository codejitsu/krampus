// Copyright (C) 2017, codejitsu.

package utils

import com.typesafe.config.{Config, ConfigFactory}

class AppConfig() {

  lazy val config: Config = ConfigFactory.load()

  lazy val kafkaConfig: Config = config.getConfig("krampus.web-app.kafka")
  lazy val systemName: String = config.getString("krampus.web-app.system-name")
}

