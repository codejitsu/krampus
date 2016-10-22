// Copyright (C) 2016, codejitsu.

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
    Source.fromIterator(() => source.getLines())
  }
}
