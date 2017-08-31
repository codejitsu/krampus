// Copyright (C) 2017, codejitsu.

package krampus.producer.json

import akka.NotUsed
import akka.stream.scaladsl._
import krampus.producer.WikiProducer

object JsonFileProducer extends WikiProducer {
  def main(args: Array[String]): Unit = {
    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, NotUsed] = {
    import scala.io.{Source => ioSource}

    val source = ioSource.fromFile(args.head)
    Source.fromIterator(() => source.getLines())
  }
}
