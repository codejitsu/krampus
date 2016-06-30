// Copyright (C) 2016, codejitsu.

package krampus.entity

import org.joda.time.DateTime
import org.scalacheck.{Prop, Properties}

class WikiChangeEntrySpecification extends Properties("WikiChangeEntry") {
  import CommonGenerators._

  import Prop.forAll

  property("constructor: id") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.id.toString == avroEntry.getId.toString
  }

  property("constructor: added") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.added == avroEntry.getAdded
  }

  property("constructor: channel") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.channel == avroEntry.getChannel.toString
  }

  property("constructor: comment") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.comment == avroEntry.getComment.toString
  }

  property("constructor: deleted") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.deleted == avroEntry.getDeleted
  }

  property("constructor: delta") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.delta == avroEntry.getDelta
  }

  property("constructor: diffUrl") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.diffUrl.toString == avroEntry.getDiffUrl
  }

  property("constructor: flags") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.flags == avroEntry.getFlags.toString.split(",").toList
  }

  property("constructor: isMinor") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isMinor == avroEntry.getIsMinor
  }

  property("constructor: isNew") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isNew == avroEntry.getIsNew
  }

  property("constructor: isRobot") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isRobot == avroEntry.getIsRobot
  }

  property("constructor: isUnpatrolled") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.isUnpatrolled == avroEntry.getIsUnpatrolled
  }

  property("constructor: namespace") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.namespace == avroEntry.getNamespace.toString
  }

  property("constructor: page") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.page == avroEntry.getPage.toString
  }

  property("constructor: timestamp") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.timestamp == new DateTime(avroEntry.getTimestamp.toString)
  }

  property("constructor: user") = forAll(wikiChangeEntryAvroGenerator) { avroEntry =>
    val converted = WikiChangeEntry(avroEntry)

    converted.user == avroEntry.getUser.toString
  }
}