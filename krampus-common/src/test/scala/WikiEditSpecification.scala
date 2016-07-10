// Copyright (C) 2016, codejitsu.

package krampus.entity

import org.joda.time.DateTime
import org.scalacheck.{Prop, Properties}

class WikiEditSpecification extends Properties("WikiEdit") {
  import CommonGenerators._

  import Prop.forAll

  property("constructor: id") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.id.toString == avroEntry.getId.toString
  }

  property("constructor: added") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.added == avroEntry.getAdded
  }

  property("constructor: channel") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.channel == avroEntry.getChannel.toString
  }

  property("constructor: comment") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.comment == avroEntry.getComment.toString
  }

  property("constructor: deleted") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.deleted == avroEntry.getDeleted
  }

  property("constructor: delta") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.delta == avroEntry.getDelta
  }

  property("constructor: diffUrl") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.diffUrl.toString == avroEntry.getDiffUrl
  }

  property("constructor: flags") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.flags == avroEntry.getFlags.toString.split(",").toList
  }

  property("constructor: isMinor") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.isMinor == avroEntry.getIsMinor
  }

  property("constructor: isNew") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.isNew == avroEntry.getIsNew
  }

  property("constructor: isRobot") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.isRobot == avroEntry.getIsRobot
  }

  property("constructor: isUnpatrolled") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.isUnpatrolled == avroEntry.getIsUnpatrolled
  }

  property("constructor: namespace") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.namespace == avroEntry.getNamespace.toString
  }

  property("constructor: page") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.page == avroEntry.getPage.toString
  }

  property("constructor: timestamp") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.timestamp == new DateTime(avroEntry.getTimestamp.toString)
  }

  property("constructor: user") = forAll(wikiEditAvroGenerator) { avroEntry =>
    val converted = WikiEdit(avroEntry)

    converted.user == avroEntry.getUser.toString
  }
}