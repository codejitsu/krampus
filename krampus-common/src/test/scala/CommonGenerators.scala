// Copyright (C) 2017, codejitsu.

package krampus.entity

import java.io.ByteArrayOutputStream

import krampus.avro.WikiEditAvro
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.scalacheck.Gen

object CommonGenerators {
  val noOfDays = 23

  val now = DateTime.now()
  val dates = for {
    t0 <- 0 until noOfDays
  } yield {
    now.minusDays(t0)
  }

  def sameDayTimestampGen(date: DateTime): Gen[DateTime] = for {
    hourOfDay <- Gen.choose(0, 23) // scalastyle:ignore
    minuteOfHour <- Gen.choose(0, 59) // scalastyle:ignore
  } yield {
    val dtz = DateTimeZone.forID("Europe/Berlin")
    val ldt = new LocalDateTime(date.getMillis, dtz).withTime(hourOfDay, minuteOfHour, 0, 0)
    ldt.toDateTime(DateTimeZone.UTC)
  }

  val timestampGen: Gen[DateTime] = for {
    date: DateTime <- Gen.oneOf(dates)
    timestamp <- sameDayTimestampGen(date)
  } yield timestamp


  val wikiEditAvroGenerator: Gen[WikiEditAvro] = for {
    uuid <- Gen.uuid
    isRobot <- Gen.oneOf(true, false)
    channel <- Gen.alphaStr
    timestamp <- timestampGen
    flags <- Gen.alphaStr
    isUnpatrolled <- Gen.oneOf(true, false)
    page <- Gen.alphaStr
    diffUrl <- Gen.identifier
    added <- Gen.posNum[Int]
    deleted <- Gen.posNum[Int]
    comment <- Gen.alphaStr
    isNew <- Gen.oneOf(true, false)
    isMinor <- Gen.oneOf(true, false)
    delta <- Gen.posNum[Int]
    user <- Gen.alphaStr
    namespace <- Gen.alphaStr
  } yield new WikiEditAvro(uuid.toString, isRobot, channel, timestamp.toString, flags, isUnpatrolled, page,
    s"http://$diffUrl.com/diff", added, deleted, comment, isNew, isMinor, delta, user, namespace)

  val rawKafkaMessageGenerator: Gen[(RawKafkaMessage, WikiEdit)] = for {
    entity <- wikiEditAvroGenerator
  } yield {
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null) // scalastyle:ignore
    val writer = new SpecificDatumWriter[WikiEditAvro](WikiEditAvro.getClassSchema())

    writer.write(entity, encoder)
    encoder.flush()
    out.close()
    val serializedAvro = out.toByteArray()

    (RawKafkaMessage(entity.getChannel.toString.toCharArray.map(_.toByte), serializedAvro), WikiEdit(entity))
  }
}
