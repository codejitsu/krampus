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

  object CompileDeps {
    val config        = "com.typesafe"                    % "config"                   % TypesafeConfigVer
    val logback       = "ch.qos.logback"                  % "logback-classic"          % LogbackVer
    val logging       = "com.typesafe.scala-logging"     %% "scala-logging"            % TypesafeLoggingVer
    val joda          = "joda-time"                       % "joda-time"                % JodaTimeVer
    val jodaConvert   = "org.joda"                        % "joda-convert"             % JodaTimeConvertVer
    val akkaStreams   = "com.typesafe.akka"              %% "akka-stream-experimental" % AkkaStreamsVer excludeAll
      (ExclusionRule("com.typesafe.akka", "akka-actor_2.11"), ExclusionRule("com.typesafe", "config"))

    val akka          = "com.typesafe.akka"              %% "akka-actor"               % AkkaVer
    val akkaLogger    = "com.typesafe.akka"               % "akka-slf4j_2.11"          % AkkaSlf4jVer
    val jackson       = "org.json4s"                     %% "json4s-jackson"           % Jackson4sVer
    val avro          = "org.apache.avro"                 % "avro"                     % AvroVer
    val kafkaClients  = "org.apache.kafka"                % "kafka-clients"            % KafkaClientsVer
    val reactiveKafka = "com.softwaremill.reactivekafka" %% "reactive-kafka-core"      % ReactiveKafkaVer excludeAll
      (ExclusionRule(organization = "com.typesafe.akka"))

    val phantom       = "com.websudos"                   %% "phantom-dsl"              % PhantomVer

    val akkaLogging  =        "com.typesafe.akka"        %% "akka-slf4j"              % AkkaLoggingVer

    val webjarsPlay =         "org.webjars"              %% "webjars-play"            % WebjarsPlayVer
    val webjarsAng =          "org.webjars"               % "angularjs"               % WebjarsAngVer
    val webjarsAngRoute =     "org.webjars"               % "angular-ui-router"       % WebjarsAngRouteVer
    val webjarsAngWebsocket = "org.webjars.bower"         % "angular-websocket"       % WebjarsAngWebsocketVer

    val webjarsBootstrap =    "org.webjars"               % "bootstrap"               % WebjarsBootstrapVer
  }

  object TestDeps {
    val scalatest     = "org.scalatest"           %% "scalatest"                % ScalaTestVer      % Test
    val scalacheck    = "org.scalacheck"          %% "scalacheck"               % ScalaCheckVer     % Test
    val akkatest      = "com.typesafe.akka"       %% "akka-testkit"             % AkkaVer           % Test
    val embeddedKafka = "net.manub"               %% "scalatest-embedded-kafka" % EmbeddedKafkaVer  % Test
  }

  import CompileDeps._

  val test = Seq(TestDeps.scalatest, TestDeps.scalacheck)

  /** Module deps */

  val krampusCommon = Seq(config, joda, jodaConvert, avro) ++ test
  val krampusMetrics = Seq(config, akka, akkaStreams, reactiveKafka, logging, logback)
  val krampusProcessor = Seq(config, akka, akkaLogger, akkaStreams, reactiveKafka, logging, logback, kafkaClients, phantom) ++ Seq(TestDeps.akkatest, TestDeps.embeddedKafka)
  val krampusProducer = Seq(config, akka, akkaStreams, jackson, kafkaClients, reactiveKafka, logging, logback)
  val krampusScoreApp = Seq(config)
  val krampusSparkApp = Seq(config)
  val krampusWebApp = Seq(akka, akkaStreams, reactiveKafka, logging, logback, akkaLogging,
    webjarsPlay, webjarsAng, webjarsAngRoute, webjarsAngWebsocket, webjarsBootstrap)
}
