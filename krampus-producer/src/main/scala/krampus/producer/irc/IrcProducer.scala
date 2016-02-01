// Copyright (C) 2015, codejitsu.

package krampus.producer.irc

import akka.actor.{ActorRef, Actor, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.Source
import com.metamx.common.scala.Jackson
import io.imply.wikiticker.{IrcTicker, Message, MessageListener}
import krampus.producer.WikiProducer
import krampus.producer.irc.IrcPublisher.Publish

class IrcPublisher() extends ActorPublisher[String] {
  import scala.collection._

  var queue: mutable.Queue[String] = mutable.Queue()

  override def receive: Actor.Receive = {
    case Publish(s) =>
      println("Push msg in the queue.")
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
      println("msg -> flow")
      onNext(queue.dequeue())
    }
  }
}

object IrcPublisher {
  case class Publish(data: String)
}

class WikiRun(wikis: Seq[String], actor: ActorRef) extends Runnable {
  val listener = new MessageListener {
    override def process(message: Message) = {
      actor ! Publish(Jackson.generate(message.toMap))
    }
  }

  val ticker = new IrcTicker(
    "irc.wikimedia.org",
    "imply",
    wikis map (x => s"#$x.wikipedia"),
    Seq(listener)
  )

  ticker.start()
  Thread.sleep(30000)

  override def run(): Unit = {
    while(true) {
      Thread.sleep(10)
    }
  }
}

/**
  * IRC based wikipedia entry producer.
  */
object IrcProducer extends WikiProducer {
  val dataPublisherRef = system.actorOf(Props[IrcPublisher])
  val dataPublisher = ActorPublisher[String](dataPublisherRef)

  def main(args: Array[String]): Unit = {
    val wikipedias = args.head.split(",")

    val ircThread = new Thread(new WikiRun(wikipedias.toSeq, dataPublisherRef))

    ircThread.start()

    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, Unit] = {
    Source.fromPublisher(dataPublisher)
  }
}
