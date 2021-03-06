// Copyright (C) 2017, codejitsu.

import play.sbt.PlayScala
import sbt.{ExclusionRule, _}
import sbt.Keys._
import sbtdocker.DockerPlugin

object ProjectBuild extends Build {
  import Settings._

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = parentSettings,
    aggregate = Seq(krampusCommon, krampusSource, krampusMetrics, krampusProcessor, krampusProducer, krampusScoreApp, krampusSparkApp,
      krampusWebApp)
  )

  lazy val krampusCommon = Project(
    id = "krampus-common",
    base = file("./krampus-common"),
    settings = commonSettings ++ Seq(libraryDependencies ++= Dependencies.krampusCommon)
  )

  lazy val krampusSource = Project(
    id = "krampus-source",
    base = file("./krampus-source"),
    settings = defaultSettings ++ krampusSourceSettings ++ Seq(libraryDependencies ++= Dependencies.krampusSource)
  ).enablePlugins(DockerPlugin)

  lazy val krampusMetrics = Project(
    id = "krampus-metrics-aggregator",
    base = file("./krampus-metrics-aggregator"),
    settings = defaultSettings ++ krampusMetricsSettings ++ Seq(libraryDependencies ++= Dependencies.krampusMetrics)
  ).dependsOn(krampusCommon).enablePlugins(DockerPlugin)

  lazy val krampusProcessor = Project(
    id = "krampus-processor",
    base = file("./krampus-processor"),
    settings = defaultSettings ++ phantomSettings ++ krampusProcessorSettings ++ Seq(libraryDependencies ++= Dependencies.krampusProcessor)
  ).dependsOn(krampusCommon % "compile->compile;test->test").enablePlugins(DockerPlugin)

  lazy val krampusProducer = Project(
    id = "krampus-producer",
    base = file("./krampus-producer"),
    settings = defaultSettings ++ krampusProducerSettings ++ Seq(libraryDependencies ++= Dependencies.krampusProducer)
  ).dependsOn(krampusCommon).enablePlugins(DockerPlugin)

  lazy val krampusScoreApp = Project(
    id = "krampus-score-app",
    base = file("./krampus-score-app"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusScoreApp)
  ).dependsOn(krampusCommon)

  lazy val krampusSparkApp = Project(
    id = "krampus-spark-app",
    base = file("./krampus-spark-app"),
    settings = defaultSettings ++ sparkAppSettings ++ Seq(libraryDependencies ++= Dependencies.krampusSparkApp)
  )

  lazy val krampusWebApp = Project(
    id = "krampus-web-app",
    base = file("./krampus-web-app"),
    settings = defaultSettings ++ krampusWebAppSettings ++ Seq(libraryDependencies ++= Dependencies.krampusWebApp)
  ).enablePlugins(PlayScala).dependsOn(krampusCommon).enablePlugins(DockerPlugin)
}

object Dependencies {
  import Versions._

  object CompileDeps {
    val config        = "com.typesafe"                    % "config"                   % TypesafeConfigVer
    val logback       = "ch.qos.logback"                  % "logback-classic"          % LogbackVer
    val logging       = "com.typesafe.scala-logging"     %% "scala-logging"            % TypesafeLoggingVer
    val joda          = "joda-time"                       % "joda-time"                % JodaTimeVer
    val jodaConvert   = "org.joda"                        % "joda-convert"             % JodaTimeConvertVer
    val akkaStreams   = "com.typesafe.akka"              %% "akka-stream"              % AkkaVer

    val akka          = "com.typesafe.akka"              %% "akka-actor"               % AkkaVer
    val akkaLogger    = "com.typesafe.akka"              %% "akka-slf4j"               % AkkaSlf4jVer
    val jackson       = "org.json4s"                     %% "json4s-jackson"           % Jackson4sVer
    val avro          = "org.apache.avro"                 % "avro"                     % AvroVer
    val reactiveKafka = "com.softwaremill.reactivekafka" %% "reactive-kafka-core"      % ReactiveKafkaVer excludeAll
      (ExclusionRule(organization = "com.typesafe.akka"))

    val phantom       = "com.websudos"                   %% "phantom-dsl"              % PhantomVer

    val webjarsPlay =         "org.webjars"              %% "webjars-play"            % WebjarsPlayVer
    val webjarsAng =          "org.webjars"               % "angularjs"               % WebjarsAngVer
    val webjarsAngRoute =     "org.webjars"               % "angular-ui-router"       % WebjarsAngRouteVer
    val webjarsAngWebsocket = "org.webjars.bower"         % "angular-websocket"       % WebjarsAngWebsocketVer

    val webjarsBootstrap =    "org.webjars"               % "bootstrap"               % WebjarsBootstrapVer

    val sparkCore =   "org.apache.spark"                 %% "spark-core"              % SparkVer
    val sparkMl   =   "org.apache.spark"                 %% "spark-mllib"             % SparkMlVer
    val sparkCassandraConnector = "com.datastax.spark"   %% "spark-cassandra-connector" % SparkCassandraVer

    val guava =               "com.google.guava"          % "guava"                   % GuavaVer
    val scalastic =           "org.scalactic"            %% "scalactic"               % ScalasticVer

    val scalaUtil =           "com.metamx"               %% "scala-util"              % ScalaUtilVer exclude("log4j", "log4j") force()
    val ircApi =              "com.ircclouds.irc"         % "irc-api"                 % IrcApiVer
  }

  object TestDeps {
    val scalatest     = "org.scalatest"           %% "scalatest"                % ScalaTestVer      % Test
    val scalacheck    = "org.scalacheck"          %% "scalacheck"               % ScalaCheckVer     % Test
    val akkatest      = "com.typesafe.akka"       %% "akka-testkit"             % AkkaVer           % Test
    val embeddedKafka = "net.manub"               %% "scalatest-embedded-kafka" % EmbeddedKafkaVer  % Test

    val junit         = "junit"                    % "junit"                    % JunitVer          % Test
    val junitInt      = "com.novocode"             % "junit-interface"          % JunitIntVer       % Test
  }

  import CompileDeps._

  val testBase = Seq(TestDeps.scalatest, TestDeps.scalacheck)
  val testAkka = Seq(TestDeps.akkatest)
  val testKafka = Seq(TestDeps.embeddedKafka)
  val testAll = testBase ++ testAkka ++ testKafka
  val testJunit = Seq(TestDeps.junit, TestDeps.junitInt)

  /** Module deps */

  val krampusCommon = Seq(config, joda, jodaConvert, avro, akka, akkaLogger, akkaStreams, reactiveKafka, scalastic) ++ testAll
  val krampusSource = Seq(config, scalaUtil, ircApi, logback) ++ testBase
  val krampusMetrics = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback)
  val krampusProcessor = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback, phantom) ++
    Seq(TestDeps.akkatest, TestDeps.embeddedKafka, TestDeps.scalatest)
  val krampusProducer = Seq(config, akka, akkaStreams, jackson, reactiveKafka, logging, logback)
  val krampusScoreApp = Seq(config, akka, akkaStreams,
    reactiveKafka excludeAll (ExclusionRule("org.slf4j", "log4j-over-slf4j")), logging, logback, sparkCore, sparkMl)
  val krampusSparkApp = Seq(config,
    sparkCore excludeAll (ExclusionRule("io.netty", "netty-all")), sparkMl, sparkCassandraConnector, logging, logback, guava)
  val krampusWebApp = Seq(akka, akkaStreams, reactiveKafka, logging,
    webjarsPlay, webjarsAng, webjarsAngRoute, webjarsAngWebsocket, webjarsBootstrap)
}
