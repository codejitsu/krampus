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
    aggregate = Seq(krampusCommon, krampusMetrics, krampusProcessor, krampusProducer, krampusScoreApp, krampusSparkApp,
      krampusWebApp)
  )

  lazy val krampusCommon = Project(
    id = "krampus-common",
    base = file("./krampus-common"),
    settings = commonSettings ++ Seq(libraryDependencies ++= Dependencies.krampusCommon)
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
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusSparkApp)
  ).dependsOn(krampusCommon)

  lazy val krampusWebApp = Project(
    id = "krampus-web-app",
    base = file("./krampus-web-app"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.krampusWebApp)
  ).enablePlugins(PlayScala).dependsOn(krampusCommon)
}

object Dependencies {
  import Versions._

  object Compile {
    val config        = "com.typesafe"                    % "config"                   % TypesafeConfigVer
    val logback       = "ch.qos.logback"                  % "logback-classic"          % LogbackVer
    val logging       = "com.typesafe.scala-logging"     %% "scala-logging"            % TypesafeLoggingVer
    val joda          = "joda-time"                       % "joda-time"                % JodaTimeVer
    val jodaConvert   = "org.joda"                        % "joda-convert"             % JodaTimeConvertVer
    val akkaStreams   = "com.typesafe.akka"              %% "akka-stream-experimental" % AkkaStreamsVer excludeAll
      (ExclusionRule("com.typesafe.akka", "akka-actor_2.11"), ExclusionRule("com.typesafe", "config"))

    val akka          = "com.typesafe.akka"              %% "akka-actor"               % AkkaVer
    val jackson       = "org.json4s"                     %% "json4s-jackson"           % Jackson4sVer
    val avro          = "org.apache.avro"                 % "avro"                     % AvroVer
    val kafkaClients  = "org.apache.kafka"                % "kafka-clients"            % KafkaClientsVer
    val reactiveKafka = "com.softwaremill.reactivekafka" %% "reactive-kafka-core"      % ReactiveKafkaVer excludeAll
      (ExclusionRule(organization = "com.typesafe.akka"))

    val akkaLogging  =        "com.typesafe.akka"        %% "akka-slf4j"              % AkkaLoggingVer

    val webjarsPlay =         "org.webjars"              %% "webjars-play"            % WebjarsPlayVer
    val webjarsAng =          "org.webjars"               % "angularjs"               % WebjarsAngVer
    val webjarsAngRoute =     "org.webjars"               % "angular-ui-router"       % WebjarsAngRouteVer
    val webjarsAngWebsocket = "org.webjars.bower"         % "angular-websocket"       % WebjarsAngWebsocketVer

    val webjarsBootstrap =    "org.webjars"               % "bootstrap"               % WebjarsBootstrapVer
  }

  object Test {
    val scalatest     = "org.scalatest"           %% "scalatest"            % ScalaTestVer      % "test"
    val scalacheck    = "org.scalacheck"          %% "scalacheck"           % ScalaCheckVer     % "test"
    val akkatest      = "com.typesafe.akka"       %% "akka-testkit"         % AkkaVer           % "test"
  }

  import Compile._

  val test = Seq(Test.scalatest, Test.scalacheck)

  /** Module deps */

  val krampusCommon = Seq(config, joda, jodaConvert, avro) ++ test
  val krampusMetrics = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback) ++ test
  val krampusProcessor = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback) ++ test ++ Seq(Test.akkatest)
  val krampusProducer = Seq(config, akka, akkaStreams, jackson, kafkaClients, reactiveKafka, logging, logback) ++ test
  val krampusScoreApp = Seq(config) ++ test
  val krampusSparkApp = Seq(config) ++ test
  val krampusWebApp = Seq(akka, akkaStreams, reactiveKafka, logging, logback, akkaLogging,
    webjarsPlay, webjarsAng, webjarsAngRoute, webjarsAngWebsocket, webjarsBootstrap) ++ test
}
