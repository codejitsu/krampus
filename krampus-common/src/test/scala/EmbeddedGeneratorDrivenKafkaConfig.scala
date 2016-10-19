// Copyright (C) 2016, codejitsu.

package krampus.actor

import akka.util.Timeout
import net.manub.embeddedkafka.EmbeddedKafkaConfig
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

trait EmbeddedGeneratorDrivenKafkaConfig extends PatienceConfiguration {
  this: GeneratorDrivenPropertyChecks =>

  implicit val embeddedKafkaConfig = EmbeddedKafkaConfig.defaultConfig.copy(customBrokerProperties = Map("zookeeper.connection.timeout.ms" -> "30000"))
  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(15, Seconds)), interval = scaled(Span(100, Millis))) // scalastyle:ignore
  implicit val timeout: Timeout = Timeout(120 seconds)
}
