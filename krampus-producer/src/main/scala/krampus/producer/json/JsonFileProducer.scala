// Copyright (C) 2015, codejitsu.

package krampus.producer.json

import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Properties
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.config.{Config, ConfigFactory}
import krampus.avro.WikiChangeEntryAvro
import krampus.entity.WikiChangeEntry
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.{RecordMetadata, Callback, ProducerRecord, KafkaProducer}
import org.joda.time.DateTime
import org.json4s.JsonAST.{JBool, JInt, JString}
import org.json4s.jackson.JsonMethods._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object JsonFileProducer {
  def main(args: Array[String]): Unit = {
    sys.exit(run(args))
  }

  def run(args: Array[String]): Int = {
    val config = ConfigFactory.load()

    implicit val ec = ExecutionContext
      .fromExecutor(Executors.newFixedThreadPool(config.getInt("krampus.producer.json.pool-size")))

    implicit val system = ActorSystem("krampus-json-producer")
    implicit val materializer = ActorMaterializer()

    val entries = source(input(args)).via(parseJson(config))

    val producer = getProducer(config, args)
    val topic = config.getString("krampus.producer.kafka.topic")

    val graph = FlowGraph.closed(count) { implicit builder =>
      out => {
        import FlowGraph.Implicits._

        val broadcast = builder.add(Broadcast[Option[WikiChangeEntry]](2))

        entries ~> broadcast ~> logEveryNSink(logElements(config))
                   broadcast ~> toAvro() ~> serialize() ~> writeToKafka(producer, topic) ~> out
      }
    }

    graph.run().onComplete { res =>
      res match {
        case Success(c) => println(s"Completed: $c items processed")
        case Failure(_) => println("Something went wrong")
      }
      system.shutdown()
      producer.close()
    }

    0
  }

  def getProducer(config: Config, args: Array[String]): KafkaProducer[String, Array[Byte]] = {
    val props = new Properties()

    props.put("bootstrap.servers", args(1))
    props.put("acks", config.getString("krampus.producer.kafka.acks"))
    props.put("retries", config.getString("krampus.producer.kafka.retries"))
    props.put("batch.size", config.getString("krampus.producer.kafka.batch-size"))
    props.put("linger.ms", config.getString("krampus.producer.kafka.linger-ms"))
    props.put("buffer.memory", config.getString("krampus.producer.kafka.buffer-memory"))
    props.put("key.serializer", config.getString("krampus.producer.kafka.key-serializer"))
    props.put("value.serializer", config.getString("krampus.producer.kafka.value-serializer"))

    new KafkaProducer[String, Array[Byte]](props)
  }

  def input(args: Array[String]): String = args.head
  def logElements(config: Config): Int = config.getInt("krampus.producer.log.elements")

  def source(file: String): Source[String, Unit] = {
    import scala.io.{Source => ioSource}

    val source = ioSource.fromFile(file)
    Source(() => source.getLines())
  }

  def parseJson(config: Config)(implicit ec: ExecutionContext): Flow[String, Option[WikiChangeEntry], Unit] =
    Flow[String].mapAsync(config.getInt("krampus.producer.json.parallelizm"))(line => Future(parseItem(line))).collect {
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

  def count: Sink[Unit, Future[Int]] = Sink.fold(0) {
    case (c, _) => c + 1
  }

  def writeToKafka(producer: KafkaProducer[String, Array[Byte]], topic: String): Flow[Option[(CharSequence, Array[Byte])], Unit, Unit] =
    Flow[Option[(CharSequence, Array[Byte])]].map {
      case Some((key, bytes)) =>
        try {
          println(s"Writing msg to kafka ($key, bytes[${bytes.length}]), ts=${System.currentTimeMillis()}")

          val msg = new ProducerRecord[String, Array[Byte]](topic, key.toString, bytes)
          producer.send(msg, new Callback {
            override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
              if (Option(exception).isEmpty) {
                println(exception.getMessage)
              } else {
                println(s"Msg written (${msg.key()}, bytes[${msg.value().length}])")
              }
            }
          })
          println(s"After Writing msg to kafka ($key, bytes[${bytes.length}]), ts=${System.currentTimeMillis()}")
        } catch {
          case th: Throwable => println(th.getMessage)
        }

        ()

      case None => ()
    }

  def toAvro(): Flow[Option[WikiChangeEntry], Option[WikiChangeEntryAvro], Unit] =
    Flow[Option[WikiChangeEntry]].map { entryOpt =>
      entryOpt.map { entry =>
        import entry._

        val avroVal = new WikiChangeEntryAvro()

        avroVal.setIsRobot(isRobot)
        avroVal.setChannel(channel)
        avroVal.setTimestamp(timestamp.toString)
        avroVal.setFlags(flags.mkString(","))
        avroVal.setIsUnpatrolled(isUnpatrolled)
        avroVal.setPage(page)
        avroVal.setDiffUrl(diffUrl.toString)
        avroVal.setAdded(added)
        avroVal.setDeleted(deleted)
        avroVal.setComment(comment)
        avroVal.setIsNew(isNew)
        avroVal.setIsMinor(isMinor)
        avroVal.setDelta(delta)
        avroVal.setUser(user)
        avroVal.setNamespace(namespace)

        avroVal
      }
    }

  def serialize(): Flow[Option[WikiChangeEntryAvro], Option[(CharSequence, Array[Byte])], Unit] =
    Flow[Option[WikiChangeEntryAvro]].map { avroValOpt =>
      avroValOpt.map { avroVal =>
        val out = new ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        val writer = new SpecificDatumWriter[WikiChangeEntryAvro](WikiChangeEntryAvro.getClassSchema())

        writer.write(avroVal, encoder)
        encoder.flush()
        out.close()
        (avroVal.getChannel(), out.toByteArray())
      }
    }
}
