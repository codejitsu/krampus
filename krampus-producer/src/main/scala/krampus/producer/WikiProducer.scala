// Copyright (C) 2016, codejitsu.

package krampus.producer

import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._
import com.softwaremill.react.kafka.{ProducerProperties, ReactiveKafka}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import kafka.serializer.Encoder
import krampus.avro.WikiEditAvro
import krampus.entity.WikiEdit
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.joda.time.DateTime
import org.json4s.JsonAST.{JBool, JInt, JString}
import org.json4s.jackson.JsonMethods._
import org.reactivestreams.Subscriber

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Abstract wikipedia entry producer.
  */
abstract class WikiProducer extends LazyLogging {
  val config = ConfigFactory.load()

  logger.info(config.toString)

  implicit val ec = ExecutionContext
      .fromExecutor(Executors.newFixedThreadPool(config.getInt("krampus.producer.json.pool-size")))

  implicit val system = ActorSystem("krampus-producer")

  implicit val materializer = ActorMaterializer.create(system)

  def run(args: Array[String]): Int = {

    args.map(logger.info(_))


    val entries = source(args).via(parseJson(config))

    val graph = GraphDSL.create(count) { implicit builder =>
      out => {
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Option[WikiEdit]](3))

        entries ~> broadcast ~> logEveryNSink(logElements(config))
                   broadcast ~> out
                   broadcast ~> avro ~> serialize.collect {
                     case Some(msg) => msg
                   } ~> Sink.fromSubscriber(kafkaSink(config, args, new ReactiveKafka()))

        ClosedShape
      }
    }

    RunnableGraph.fromGraph(graph).run().onComplete { res =>
      res match {
        case Success(c) => logger.info(s"Completed: $c items processed")
        case Failure(_) => logger.info("Something went wrong")
      }
      system.terminate()
    }

    0
  }

  def kafkaSink(config: Config, args: Array[String],
                kafka: ReactiveKafka)(implicit system: ActorSystem): Subscriber[(CharSequence, Array[Byte])] = {
    val properties: ProducerProperties[(CharSequence, Array[Byte])] = ProducerProperties(
      brokerList = args(1),
      topic = config.getString("krampus.producer.kafka.topic"),
      clientId = UUID.randomUUID().toString,
      encoder = new Encoder[(CharSequence, Array[Byte])]() {
        override def toBytes(t: (CharSequence, Array[Byte])): Array[Byte] = t._2
      },
      partitionizer = msg => Option(msg._1.toString.toCharArray.map(_.toByte))
    )

    kafka.publish(properties.requestRequiredAcks(config.getInt("krampus.producer.kafka.acks")))
  }

  def input(args: Array[String]): String = args.head
  def logElements(config: Config): Int = config.getInt("krampus.producer.log.elements")

  def source(args: Array[String]): Source[String, Unit]

  def parseJson(config: Config)(implicit ec: ExecutionContext): Flow[String, Option[WikiEdit], Unit] =
    Flow[String].mapAsync(config.getInt("krampus.producer.json.parallelizm"))(line => Future(parseItem(line))).collect {
      case Success(e) => Some(e)
      case Failure(f) => {
        logger.error(s"Exception: ${f.getMessage}\n")
        None
      }
    }

  def parseItem(line: String): Try[WikiEdit] = Try {
    val json = parse(line)

    val parsed: List[WikiEdit] = for {
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
      WikiEdit(UUID.randomUUID(), isRobot, channel, new DateTime(timestamp), flags.split(",").toList,
        isUnpatrolled, page, new URL(diffUrl), added.toInt, deleted.toInt,
        comment, isNew, isMinor, delta.toInt, user, namespace)
    }

    parsed.head
  }

  def logEveryNSink[T](n: Int): Sink[T, Future[Int]] = Sink.fold(0) { (x, y: T) =>
    if (x % n == 0) {
      logger.info(s"element #$x -> $y\n")
    }

    x + 1
  }

  def count: Sink[Option[WikiEdit], Future[Int]] = Sink.fold(0) {
    case (c, _) => c + 1
  }

  def writeToKafka(): Flow[(CharSequence, Array[Byte]), Unit, Unit] =
    Flow[(CharSequence, Array[Byte])].map {
      case _ => ()
    }

  def avro: Flow[Option[WikiEdit], Option[WikiEditAvro], Unit] =
    Flow[Option[WikiEdit]].map { entryOpt =>
      entryOpt.map { entry =>
        import entry._

        val avroVal = new WikiEditAvro()

        avroVal.setId(id.toString)
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

  def serialize: Flow[Option[WikiEditAvro], Option[(CharSequence, Array[Byte])], Unit] =
    Flow[Option[WikiEditAvro]].map { avroValOpt =>
      avroValOpt.map { avroVal =>
        val out = new ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null) // scalastyle:ignore
        val writer = new SpecificDatumWriter[WikiEditAvro](WikiEditAvro.getClassSchema())

        writer.write(avroVal, encoder)
        encoder.flush()
        out.close()
        (avroVal.getChannel(), out.toByteArray())
      }
    }
}
