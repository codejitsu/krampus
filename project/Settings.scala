// Copyright (C) 2016, codejitsu.

import de.johoop.jacoco4sbt.JacocoPlugin.jacoco
import de.johoop.jacoco4sbt.Thresholds
import org.scalastyle.sbt.ScalastylePlugin
import com.websudos.phantom.sbt.PhantomSbtPlugin
import sbt._
import sbt.Keys._
import scala.language.postfixOps
import sbtassembly.AssemblyPlugin.autoImport._

object Settings extends Build {
  lazy val buildSettings = Seq(
    name                  := "krampus",
    normalizedName        := "krampus",
    organization          := "codejitsu",
    organizationHomepage  := Some(url("http://www.codejitsu.net")),
    scalaVersion          := Versions.ScalaVer,
    homepage              := Some(url("http://www.github.com/codejitsu/krampus"))
  )

  override lazy val settings = super.settings ++ buildSettings

  val parentSettings = buildSettings ++ Seq(
    publishArtifact := false,
    publish         := {}
  )

  val scalacSettings = Seq("-encoding", "UTF-8", s"-target:jvm-${Versions.JDKVer}", "-feature", "-language:_",
    "-deprecation", "-unchecked", "-Xfatal-warnings", "-Xlint")

  val javacSettings = Seq("-encoding", "UTF-8", "-source", Versions.JDKVer,
    "-target", Versions.JDKVer, "-Xlint:deprecation", "-Xlint:unchecked")

  lazy val defaultSettings = Defaults.coreDefaultSettings ++ testSettings ++ Seq(
    autoCompilerPlugins := true,
    scalacOptions       ++= scalacSettings,
    javacOptions        in Compile    ++= javacSettings,
    ivyLoggingLevel     in ThisBuild  := UpdateLogging.Quiet,
    parallelExecution   in ThisBuild  := false,
    parallelExecution   in Global     := false,
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
    resolvers += "Typesafe private" at "https://private-repo.typesafe.com/typesafe/maven-releases",
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    resolvers ++= Seq(
      Resolver.bintrayRepo("websudos", "oss-releases"),
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
  )

  lazy val commonSettings = defaultSettings ++ sbtavro.SbtAvro.avroSettings

  val tests = inConfig(Test)(Defaults.testTasks) ++ inConfig(IntegrationTest)(Defaults.itSettings)

  val testOptionSettings = Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
    Tests.Argument(TestFrameworks.JUnit, "-oDF", "-v", "-a")
  )

  lazy val testSettings = tests ++ jacoco.settings ++ Seq(
    parallelExecution in Test             := false,
    parallelExecution in IntegrationTest  := false,
    testOptions       in Test             ++= testOptionSettings,
    testOptions       in IntegrationTest  ++= testOptionSettings,
    fork              in Test             := true,
    fork              in IntegrationTest  := true,
    (compile in IntegrationTest)        <<= (compile in Test, compile in IntegrationTest) map { (_, c) => c },
    managedClasspath in IntegrationTest <<= Classpaths.concat(managedClasspath in IntegrationTest, exportedProducts in Test),
    jacoco.thresholds in jacoco.Config := Thresholds(instruction = 70, method = 70, branch = 70, complexity = 70, line = 70, clazz = 70)
  )

  lazy val phantomSettings = PhantomSbtPlugin.projectSettings

  lazy val krampusProcessorSettings = Seq(
    javaOptions += "-Xmx4G",
    javaOptions += "-Xms4G"
  )

  lazy val sparkAppSettings = Seq(
    // Drop these jars
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      val excludes = Set(
        "javax.inject-2.4.0-b34.jar",
        "aopalliance-1.0.jar",
        "commons-beanutils-core-1.8.0.jar",
        "commons-beanutils-1.8.0.jar",
        "logback-classic-1.1.7.jar"
      )
      cp filter { jar => excludes(jar.data.getName) }
    },

    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.discard
      case old if old.endsWith("package-info.class") => MergeStrategy.first
      case old if old.endsWith("io.netty.versions.properties") => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("com.google.**" -> "shadeio.@1").inAll
    )
  )

  lazy val krampusSourceSettings = Seq(
    mainClass in assembly := Some("io.imply.wikiticker.ConsoleTicker"),

    // Resolve duplicates for Sbt Assembly
    assemblyMergeStrategy in assembly := {
      case PathList(xs@_*) if xs.last == "io.netty.versions.properties" => MergeStrategy.rename
      case other => (assemblyMergeStrategy in assembly).value(other)
    },
    
    // publish to artifacts directory
    publishArtifact in(Compile, packageDoc) := false,

    publishTo := Some(Resolver.file("file", new File("artifacts"))),

    cleanFiles <+= baseDirectory { base => base / "artifacts" }
  )
}
