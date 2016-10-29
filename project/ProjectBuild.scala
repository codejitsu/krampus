// Copyright (C) 2016, codejitsu.

import play.sbt.PlayScala
import sbt._
import sbt.Keys._

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
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusSource)
  )

  lazy val krampusMetrics = Project(
    id = "krampus-metrics-aggregator",
    base = file("./krampus-metrics-aggregator"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusMetrics)
  ).dependsOn(krampusCommon)

  lazy val krampusProcessor = Project(
    id = "krampus-processor",
    base = file("./krampus-processor"),
    settings = defaultSettings ++ phantomSettings ++ Seq(libraryDependencies ++= Dependencies.krampusProcessor)
  ).dependsOn(krampusCommon % "compile->compile;test->test")

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
    settings = defaultSettings ++ sparkAppSettings ++ Seq(libraryDependencies ++= Dependencies.krampusSparkApp)
  )

  lazy val krampusWebApp = Project(
    id = "krampus-web-app",
    base = file("./krampus-web-app"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusWebApp)
  ).enablePlugins(PlayScala).dependsOn(krampusCommon)
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
    val scalastic = "org.scalactic"                      %% "scalactic"               % ScalasticVer

    val scalaUtil =  "com.metamx" %% "scala-util" % "1.11.3" exclude("log4j", "log4j") force()
    val ircApi = "com.ircclouds.irc" % "irc-api" % "1.0-0014"
    val logbackCore2 = "ch.qos.logback" % "logback-core" % "1.1.2" % "test"
    val logbackClassic2 = "ch.qos.logback" % "logback-classic" % "1.1.2" % "test"
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

  val test = Seq(TestDeps.scalatest, TestDeps.scalacheck, TestDeps.akkatest, TestDeps.embeddedKafka)
  val testJunit = Seq(TestDeps.junit, TestDeps.junitInt)

  /** Module deps */

  val krampusCommon = Seq(config, joda, jodaConvert, avro, akka, akkaLogger, akkaStreams, reactiveKafka, scalastic) ++ test
  val krampusSource = Seq(scalaUtil, ircApi, logbackCore2, logbackClassic2) ++ testJunit ++ test
  val krampusMetrics = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback)
  val krampusProcessor = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback, phantom) ++ Seq(TestDeps.akkatest, TestDeps.embeddedKafka)
  val krampusProducer = Seq(config, akka, akkaStreams, jackson, reactiveKafka, logging, logback)
  val krampusScoreApp = Seq(config, akka, akkaStreams,
    reactiveKafka excludeAll (ExclusionRule("org.slf4j", "log4j-over-slf4j")), logging, logback, sparkCore, sparkMl)
  val krampusSparkApp = Seq(config,
    sparkCore excludeAll (ExclusionRule("io.netty", "netty-all")), sparkMl, sparkCassandraConnector, logging, logback, guava)
  val krampusWebApp = Seq(akka, akkaStreams, reactiveKafka, logging,
    webjarsPlay, webjarsAng, webjarsAngRoute, webjarsAngWebsocket, webjarsBootstrap)
}
