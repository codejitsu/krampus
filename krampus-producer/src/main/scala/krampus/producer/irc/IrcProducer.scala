// Copyright (C) 2015, codejitsu.

package krampus.producer.irc

import akka.actor.{Actor, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.Source
import com.metamx.common.lifecycle.Lifecycle
import com.metamx.common.scala.lifecycle._
import com.metamx.common.scala.Jackson
import com.typesafe.scalalogging.LazyLogging
import io.imply.wikiticker.ConsoleTicker._
import io.imply.wikiticker.{IrcTicker, Message, MessageListener}
import krampus.producer.WikiProducer
import krampus.producer.irc.IrcPublisher.Publish

class IrcPublisher() extends ActorPublisher[String] with LazyLogging {
  import scala.collection._

  val queue: mutable.Queue[String] = mutable.Queue()

  override def receive: Actor.Receive = {
    case Publish(s) =>
      logger.info("Push msg in the queue.")
      queue.enqueue(s)
      publishIfNeeded()

    case Request(cnt) =>
      logger.info("Request: " + cnt)
      publishIfNeeded()

    case Cancel =>
      context.stop(self)

    case _ =>
  }

  def publishIfNeeded(): Unit = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      logger.info("msg -> flow")
      onNext(queue.dequeue())
    }
  }
}

object IrcPublisher {
  case class Publish(data: String)
}

/**
  * IRC based wikipedia entry producer.
  */
object IrcProducer extends WikiProducer {
  val dataPublisherRef = system.actorOf(Props[IrcPublisher])
  val dataPublisher = ActorPublisher[String](dataPublisherRef)

  def main(args: Array[String]): Unit = {
    val wikis = args.head.split(",")

    val listener = new MessageListener {
      override def process(message: Message) = {
        dataPublisherRef ! Publish(Jackson.generate(message.toMap))
      }
    }

    val ticker = new IrcTicker(
      "irc.wikimedia.org",
      "imply",
      wikis map (x => s"#$x.wikipedia"),
      Seq(listener)
    )

    val lifecycle = new Lifecycle

    lifecycle onStart {
      ticker.start()
    } onStop {
      ticker.stop()
    }

    try {
      lifecycle.start()
    } catch {
      case e: Throwable =>
        log.error(e, "Failed to start up, stopping and exiting.")
        lifecycle.stop()
    }

    Thread.sleep(30000)

    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, Unit] = {
    Source.fromPublisher(dataPublisher)
  }
}
