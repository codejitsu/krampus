// Copyright (C) 2015, codejitsu.

package krampus.producer.irc

import akka.actor.{Props, ActorSystem, Actor}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.Source
import com.metamx.common.lifecycle.Lifecycle
import com.metamx.common.scala.Jackson
import io.imply.wikiticker.ConsoleTicker._
import io.imply.wikiticker.{IrcTicker, Message, MessageListener}
import krampus.producer.WikiProducer
import com.metamx.common.scala.lifecycle._
import krampus.producer.irc.IrcPublisher.Publish

class IrcPublisher extends ActorPublisher[String] {
  import scala.collection._

  var queue: mutable.Queue[String] = mutable.Queue()

  override def receive: Actor.Receive = {
    case Publish(s) =>
      queue.enqueue(s)
      publishIfNeeded()

    case Request(cnt) =>
      println("Request: " + cnt)

      publishIfNeeded()

    case Cancel =>
      context.stop(self)

    case _ =>
  }

  def publishIfNeeded(): Unit = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      println("onNext")
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
  def main(args: Array[String]): Unit = {
    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, Unit] = {
    implicit val system = ActorSystem()

    val dataPublisherRef = system.actorOf(Props[IrcPublisher])
    val dataPublisher = ActorPublisher[String](dataPublisherRef)

    val wikipedias = args.head.split(",")

    val listener = new MessageListener {
      override def process(message: Message) = {
        dataPublisherRef ! Publish(Jackson.generate(message.toMap))
      }
    }

    val ticker = new IrcTicker(
      "irc.wikimedia.org",
      "imply",
      wikipedias map (x => s"#$x.wikipedia"),
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
      lifecycle.join()
    } catch {
      case e: Throwable =>
        log.error(e, "Failed to start up, stopping and exiting.")
        lifecycle.stop()
    }

    Source.fromPublisher(dataPublisher)
  }
}
