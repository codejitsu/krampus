// Copyright (C) 2015, codejitsu.

package krampus.producer.json

import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.config.{Config, ConfigFactory}
import krampus.entity.WikiChangeEntry
import org.joda.time.DateTime
import org.json4s.JsonAST.{JBool, JInt, JString}
import org.json4s.jackson.JsonMethods._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object JsonFileProducer {
  def main(args: Array[String]): Unit = {
    sys.exit(run(args))
  }

  def run(args: Array[String]): Int = {
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val system = ActorSystem("krampus-json-producer")
    implicit val materializer = ActorMaterializer()

    val config = ConfigFactory.load()

    val entries = source(input(args)).via(parseJson())

    val graph = FlowGraph.closed(count) { implicit builder =>
      out => {
        import FlowGraph.Implicits._

        val broadcast = builder.add(Broadcast[Option[WikiChangeEntry]](2))

        entries ~> broadcast ~> logEveryNSink(logElements(config))
                   broadcast ~> check() ~> out
      }
    }

    graph.run().onComplete { x =>
      x match {
        case Success(c) => println(s"Completed: $c items processed")
        case Failure(_) => println("Something went wrong")
      }
      system.shutdown()
    }
    0
  }

  def input(args: Array[String]): String = args.head
  def logElements(config: Config): Int = config.getInt("krampus.producer.log.elements")

  def source(file: String): Source[String, Unit] = {
    import scala.io.{Source => ioSource}

    val source = ioSource.fromFile(file)
    Source(() => source.getLines())
  }

  def parseJson(): Flow[String, Option[WikiChangeEntry], Unit] = Flow[String].map(parseItem).collect {
    case Success(e) => Some(e)
    case Failure(f) => {
      println(s"Exception: ${f.getMessage}\n")
      None
    }
  }

  def parseItem(line: String): Try[WikiChangeEntry] = Try {
    val json = parse(line)

    val parsed: List[WikiChangeEntry] = for {
      JBool(isRobot) <- json \ "isRobot"
      JString(channel) <- json \ "channel"
      JString(timestamp) <- json \ "timestamp"
      JString(flags) <- json \ "flags"
      JBool(isUnpatrolled) <- json \ "isUnpatrolled"
      JString(page) <- json \ "page"
      JString(diffUrl) <- json \ "diffUrl"
      JInt(added) <- json \ "added"
      JInt(deleted) <- json \ "deleted"
      JString(comment) <- json \ "comment"
      JBool(isNew) <- json \ "isNew"
      JBool(isMinor) <- json \ "isMinor"
      JInt(delta) <- json \ "delta"
      JString(user) <- json \ "user"
      JString(namespace) <- json \ "namespace"
    } yield {
      WikiChangeEntry(isRobot, channel, new DateTime(timestamp), flags.split(",").toList,
        isUnpatrolled, page, new URL(diffUrl), added.toInt, deleted.toInt,
        comment, isNew, isMinor, delta.toInt, user, namespace)
    }

    parsed.head
  }

  def logEveryNSink[T](n: Int): Sink[T, Future[Int]] = Sink.fold(0) { (x, y: T) =>
    if (x % n == 0) {
      println(s"element #$x -> $y\n")
    }

    x + 1
  }

  def count: Sink[Option[WikiChangeEntry], Future[Int]] = Sink.fold(0) {
    case (c, _) => c + 1
  }

  def check(): Flow[Option[WikiChangeEntry], Option[WikiChangeEntry], Unit] = Flow[Option[WikiChangeEntry]]
    .map(x => x)
}
