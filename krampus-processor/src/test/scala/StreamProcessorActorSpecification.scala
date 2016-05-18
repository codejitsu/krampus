// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.concurrent.Eventually
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

class StreamProcessorActorSpecification() extends TestKit(ActorSystem("StreamProcessorActorSpecification"))
  with ImplicitSender
  with FunSuiteLike
  with Matchers
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll
  with EmbeddedKafka
  with Eventually {

  test("StreamProcessorActor onMessage function receives deserialized kafka messages") {

  }
}
