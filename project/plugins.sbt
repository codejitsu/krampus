addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.6")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")

addSbtPlugin("com.cavorite" % "sbt-avro" % "0.3.2")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.6")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")

resolvers += "sbt-plugin-releases" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases"

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// phantom

resolvers ++= Seq(
  // whatever is already in here..,
  Resolver.bintrayRepo("websudos", "oss-releases"),
  Resolver.url("Outworkers OSS", url("http://dl.bintray.com/websudos/oss-releases"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.websudos" %% "phantom-sbt" % "1.22.0")
