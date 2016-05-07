// Copyright (C) 2016, codejitsu.

package krampus.entity

import krampus.avro.WikiChangeEntryAvro
import org.joda.time.DateTime
import org.scalacheck.{Gen, Prop, Properties}

class WikiChangeEntrySpecification extends Properties("WikiChangeEntry") {
  import Generators._

  import Prop.forAll

  property("constructor: added") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.added == avroEntry.getAdded
  }

  property("constructor: channel") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.channel == avroEntry.getChannel.toString
  }

  property("constructor: comment") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.comment == avroEntry.getComment.toString
  }

  property("constructor: deleted") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.deleted == avroEntry.getDeleted
  }

  property("constructor: delta") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.delta == avroEntry.getDelta
  }

  property("constructor: diffUrl") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.diffUrl.toString == avroEntry.getDiffUrl
  }

  property("constructor: flags") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.flags == avroEntry.getFlags.toString.split(",").toList
  }

  property("constructor: isMinor") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isMinor == avroEntry.getIsMinor
  }

  property("constructor: isNew") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isNew == avroEntry.getIsNew
  }

  property("constructor: isRobot") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isRobot == avroEntry.getIsRobot
  }

  property("constructor: isUnpatrolled") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isUnpatrolled == avroEntry.getIsUnpatrolled
  }

  property("constructor: namespace") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.namespace == avroEntry.getNamespace.toString
  }

  property("constructor: page") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.page == avroEntry.getPage.toString
  }

  property("constructor: timestamp") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.timestamp == new DateTime(avroEntry.getTimestamp.toString)
  }

  property("constructor: user") = forAll(avroWikiChangeEntryGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.user == avroEntry.getUser.toString
  }
}

object Generators {
  val avroWikiChangeEntryGenerator: Gen[WikiChangeEntryAvro] = for {
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
}