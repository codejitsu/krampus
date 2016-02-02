// Copyright (C) 2015, codejitsu.

package krampus.producer.irc

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.metamx.common.scala.Jackson
import com.typesafe.scalalogging.LazyLogging
import io.imply.wikiticker.{IrcTicker, Message, MessageListener}
import krampus.producer.WikiProducer
import krampus.producer.irc.IrcActorPublisher.Publish
import krampus.producer.irc.IrcReaderActor.StartReading

import scala.concurrent.Await
import scala.util.Try

class IrcReaderActor(pub: ActorRef, wikis: Array[String]) extends Actor {
  lazy val listener = new MessageListener {
    override def process(message: Message) = {
      pub ! Publish(Jackson.generate(message.toMap))
    }
  }

  lazy val ticker = new IrcTicker(
    "irc.wikimedia.org",
    "imply",
    wikis map (x => s"#$x.wikipedia"),
    Seq(listener)
  )

  override def receive: Actor.Receive = {
    case StartReading => ticker.start()
    case _ =>
  }
}

object IrcReaderActor {
  case object StartReading
}

class IrcActorPublisher() extends ActorPublisher[String] with LazyLogging {
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

object IrcActorPublisher {
  case class Publish(data: String)
}

/**
  * IRC based wikipedia entry producer.
  */
object IrcProducer extends WikiProducer {
  val dataPublisherRef = system.actorOf(Props[IrcActorPublisher])
  val dataPublisher = ActorPublisher[String](dataPublisherRef)

  var ircReader: ActorRef = _

  def main(args: Array[String]): Unit = {
    import akka.pattern.ask
    import scala.concurrent.duration._

    implicit val timeout = Timeout(1, TimeUnit.MINUTES)

    val wikis = args.head.split(",")

    ircReader = system.actorOf(Props(new IrcReaderActor(dataPublisherRef, wikis)))

    val fut = ircReader ? StartReading

    Try(Await.ready(fut, 30 seconds))

    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, Unit] = {
    Source.fromPublisher(dataPublisher)
  }
}
