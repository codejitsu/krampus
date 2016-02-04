// Copyright (C) 2016, codejitsu.

package krampus.entity

import java.net.URL

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

final case class WikiChangeEntry( isRobot: Boolean,
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
