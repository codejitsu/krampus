// Copyright (C) 2015, codejitsu.

import sbt._
import sbt.Keys._

object ProjectBuild extends Build {
  import Settings._

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = parentSettings,
    aggregate = Seq(krampusCommon, krampusMetrics, krampusProcessor, krampusProducer, krampusScoreApp, krampusSparkApp)
  )

  lazy val krampusCommon = Project(
    id = "krampus-common",
    base = file("./krampus-common"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusCommon)
  )

  lazy val krampusMetrics = Project(
    id = "krampus-metrics-aggregator",
    base = file("./krampus-metrics-aggregator"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusMetrics)
  ).dependsOn(krampusCommon)

  lazy val krampusProcessor = Project(
    id = "krampus-processor",
    base = file("./krampus-processor"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusProcessor)
  ).dependsOn(krampusCommon)

  lazy val krampusProducer = Project(
    id = "krampus-producer",
    base = file("./krampus-producer"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusProducer)
  ).dependsOn(krampusCommon)

  lazy val krampusScoreApp = Project(
    id = "krampus-score-app",
    base = file("./krampus-score-app"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusScoreApp)
  ).dependsOn(krampusCommon)

  lazy val krampusSparkApp = Project(
    id = "krampus-spark-app",
    base = file("./krampus-spark-app"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusSparkApp)
  ).dependsOn(krampusCommon)
}


object Dependencies {
  import Versions._

  object Compile {
    val config        = "com.typesafe"             % "config"                   % TypesafeConfigVer
    val logback       = "ch.qos.logback"           % "logback-classic"          % LogbackVer
    val joda          = "joda-time"                % "joda-time"                % JodaTimeVer
    val jodaConvert   = "org.joda"                 % "joda-convert"             % JodaTimeConvertVer
    val akkaStreams   = "com.typesafe.akka"       %% "akka-stream-experimental" % AkkaStreamsVer
    val jackson       = "org.json4s"              %% "json4s-jackson"           % Jackson4sVer
  }

  object Test {
    val scalatest     = "org.scalatest"           %% "scalatest"            % ScalaTestVer      % "test"
    val scalacheck    = "org.scalacheck"          %% "scalacheck"           % ScalaCheckVer     % "test"
  }

  import Compile._

  val test = Seq(Test.scalatest, Test.scalacheck)

  /** Module deps */

  val krampusCommon = Seq(config, joda, jodaConvert) ++ test
  val krampusMetrics = Seq(config) ++ test
  val krampusProcessor = Seq(config) ++ test
  val krampusProducer = Seq(config, akkaStreams, jackson) ++ test
  val krampusScoreApp = Seq(config) ++ test
  val krampusSparkApp = Seq(config) ++ test
}
