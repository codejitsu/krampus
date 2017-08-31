// Copyright (C) 2017, codejitsu.

package krampus.spark.util

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

class AppConfig(appName: String) {
  lazy val config: Config = ConfigFactory.load()

  lazy val cassandraConfig = config.getConfig(s"krampus.$appName.cassandra")
  lazy val sparkConfig = config.getConfig(s"krampus.$appName.spark")
  lazy val appConfig = config.getConfig(s"krampus.$appName")

  lazy val targetWeek: Int = appConfig.getInt("target-week")
  lazy val targetChannel: String = appConfig.getString("target-channel")
  lazy val modelDirectory: String = appConfig.getString("model-path")

  lazy val cassandraHost: String = cassandraConfig.getString("node")
  lazy val kMeans: Int = sparkConfig.getInt("k-means")
}

//TODO move this class to commons
object AppConfig {
  implicit class ConfigDuration(val c: Config) extends AnyVal {
    def getMillis(path: String): FiniteDuration =
      FiniteDuration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
}

