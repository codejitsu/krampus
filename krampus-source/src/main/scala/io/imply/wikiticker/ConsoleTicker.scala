/*
 * Copyright 2015 Imply Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.imply.wikiticker

import com.metamx.common.lifecycle.Lifecycle
import com.metamx.common.scala.Jackson
import com.metamx.common.scala.Logging
import com.metamx.common.scala.lifecycle._
import com.twitter.app.Flags
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

object ConsoleTicker extends Logging
{
  def main(args: Array[String]) {
    lazy val config = ConfigFactory
      .parseFile(new File(s"${sys.env.getOrElse("APP_CONF", ".")}/boot-configuration.conf"))
      .withFallback(ConfigFactory.load())

    val defaultWikipedias = config.getStringList("irc.channels").asScala

    val flags = new Flags("wikiticker-console")
    val out = flags("out", "-", "write to file")
    val wiki = flags("channels", defaultWikipedias.mkString(","), "wiki channels")
    flags.parseArgs(args)

    val outStream = out() match {
      case "-" => System.out
      case fileName => new PrintStream(new FileOutputStream(new File(fileName)))
    }

    val wikipedias = wiki().split(",").map(_.trim)

    val listener = new MessageListener {
      override def process(message: Message) = {
        outStream.println(Jackson.generate(message.toMap))
      }
    }

    val ticker = new IrcTicker(
      "irc.wikimedia.org",
      "imply",
      wikipedias map (x => s"#$x.wikipedia"),
      Seq(listener)
    )

    val lifecycle = new Lifecycle

    lifecycle onStart {
      ticker.start()
    } onStop {
      ticker.stop()
    }

    try {
      lifecycle.start()
      lifecycle.join()
    }
    catch {
      case e: Throwable =>
        log.error(e, "Failed to start up, stopping and exiting.")
        lifecycle.stop()
    }
  }
}
