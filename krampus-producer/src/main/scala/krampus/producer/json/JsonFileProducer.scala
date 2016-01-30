// Copyright (C) 2015, codejitsu.

package krampus.producer.json

import akka.stream.scaladsl._
import krampus.producer.WikiProducer

object JsonFileProducer extends WikiProducer {
  def main(args: Array[String]): Unit = {
    sys.exit(run(args))
  }

  override def source(args: Array[String]): Source[String, Unit] = {
    import scala.io.{Source => ioSource}

    val source = ioSource.fromFile(args.head)
    Source(() => source.getLines())
  }
}
