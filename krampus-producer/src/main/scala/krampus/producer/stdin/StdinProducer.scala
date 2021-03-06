// Copyright (C) 2017, codejitsu.

package krampus.producer.stdin

import akka.NotUsed
import akka.stream.scaladsl.Source
import krampus.producer.WikiProducer

object StdinProducer extends WikiProducer {
  def main(args: Array[String]): Unit = {
    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, NotUsed] = {
    import scala.io.{Source => ioSource}

    val source = ioSource.stdin

    logger.debug("Reading from stdin...")

    Source.fromIterator(() => source.getLines())
  }
}
