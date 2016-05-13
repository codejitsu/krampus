// Copyright (C) 2016, codejitsu.

package krampus.entity

import java.io.ByteArrayOutputStream

import krampus.avro.WikiChangeEntryAvro
import krampus.queue.RawKafkaMessage
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.scalacheck.Gen

object CommonGenerators {
  val wikiChangeEntryAvroGenerator: Gen[WikiChangeEntryAvro] = for {
    isRobot <- Gen.oneOf(true, false)
    channel <- Gen.alphaStr
    timestamp <- Gen.posNum[Int]
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
  } yield new WikiChangeEntryAvro(isRobot, channel, timestamp.toString, flags, isUnpatrolled, page,
    s"http://$diffUrl.com/diff", added, deleted, comment, isNew, isMinor, delta, user, namespace)

  val rawKafkaMessageGenerator: Gen[(RawKafkaMessage, WikiChangeEntry)] = for {
    entity <- wikiChangeEntryAvroGenerator
  } yield {
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null) // scalastyle:ignore
    val writer = new SpecificDatumWriter[WikiChangeEntryAvro](WikiChangeEntryAvro.getClassSchema())

    writer.write(entity, encoder)
    encoder.flush()
    out.close()
    val serializedAvro = out.toByteArray()

    (RawKafkaMessage(entity.getChannel.toString.toCharArray.map(_.toByte), serializedAvro), WikiChangeEntry(entity))
  }
}
