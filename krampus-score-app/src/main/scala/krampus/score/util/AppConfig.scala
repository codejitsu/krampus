// Copyright (C) 2017, codejitsu.

package krampus.score.util

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

class AppConfig() {

  lazy val config: Config = ConfigFactory.load()

  lazy val kafkaConfig: Config = config.getConfig("krampus.score-app.kafka")
  lazy val scoreConfig: Config = config.getConfig("krampus.score-app.score")
  lazy val systemName: String = config.getString("krampus.score-app.system-name")
}

object AppConfig {
  implicit class ConfigDuration(val c: Config) extends AnyVal {
    def getMillis(path: String): FiniteDuration =
      FiniteDuration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
}

