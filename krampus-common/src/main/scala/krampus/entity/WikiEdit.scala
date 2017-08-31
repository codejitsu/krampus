// Copyright (C) 2017, codejitsu.

package krampus.entity

import java.net.URL
import java.util.UUID

import krampus.avro.WikiEditAvro
import org.joda.time.DateTime

/*
  {
    "isRobot":false,
    "channel":"#en.wikipedia",
    "timestamp":"2015-12-23T17:00:01.675+01:00",
    "flags":"",
    "isUnpatrolled":false,
    "page":"National Register of Historic Places listings in Rogers County, Oklahoma",
    "diffUrl":"https://en.wikipedia.org/w/index.php?diff=696499220&oldid=679507242",
    "added":304,
    "deleted":0,
    "comment":"new listing",
    "isNew":false,
    "isMinor":false,
    "delta":304,
    "user":"Magicpiano",
    "namespace":"Main"
  }
*/

final case class WikiEdit(id: UUID,
                          isRobot: Boolean,
                          channel: String,
                          timestamp: DateTime,
                          flags: List[String],
                          isUnpatrolled: Boolean,
                          page: String,
                          diffUrl: URL,
                          added: Int,
                          deleted: Int,
                          comment: String,
                          isNew: Boolean,
                          isMinor: Boolean,
                          delta: Int,
                          user: String,
                          namespace: String )

object WikiEdit {
  def apply(avro: WikiEditAvro): WikiEdit =
    WikiEdit(
      UUID.fromString(avro.getId().toString()),
      avro.getIsRobot(),
      avro.getChannel().toString(),
      DateTime.parse(avro.getTimestamp().toString()),
      avro.getFlags().toString().split(",").toList,
      avro.getIsUnpatrolled(),
      avro.getPage().toString(),
      new URL(avro.getDiffUrl().toString()),
      avro.getAdded(),
      avro.getDeleted(),
      avro.getComment().toString(),
      avro.getIsNew(),
      avro.getIsMinor(),
      avro.getDelta(),
      avro.getUser().toString(),
      avro.getNamespace().toString()
    )
}
