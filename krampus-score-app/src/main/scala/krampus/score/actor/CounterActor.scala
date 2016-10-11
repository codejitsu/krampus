// Copyright (C) 2016, codejitsu.

package krampus.score.actor

import akka.actor.{Actor, Cancellable, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.score.ml.ML._
import org.apache.spark.mllib.clustering.KMeansModel

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

class CounterActor[T : ClassTag](name: String, flushInterval: FiniteDuration,
                                 filter: T => Boolean,
                                 statsd: StatsD,
                                 model: Option[KMeansModel],
                                 epsilon: Option[Double],
                                 startValue: Int = 0) extends Actor with LazyLogging {
  private[this] var counter: Int = startValue

  import scala.concurrent.ExecutionContext.Implicits.global

  case object Flush

  val task: Option[Cancellable] = Some(context.system.scheduler.schedule(Duration.Zero, flushInterval) {
    self ! Flush
  })

  override def receive: Receive = {
    case Flush =>
      logger.info(s"Current count '$name': $counter")

      checkCurrentCounter()

      counter = startValue

    case msg : T if filter(msg) =>
      counter = counter + 1
      statsd.increment(name)

    case x =>
      logger.error(s"Unexpected message: $x")
  }

  def checkCurrentCounter(): Unit = model match {
    case Some(kmeansModel) =>
      if (probablyAnomaly(kmeansModel, counter, epsilon, name)) {
        logger.info(s"Anomaly detected on channel $name (value $counter)")

        statsd.increment(s"$name-anomaly")
      }

    case _ =>
  }

  override def postStop(): Unit = {
    task.map(_.cancel())
    super.postStop()
  }
}

object CounterActor {
  def props[T : ClassTag](name: String, flushInterval: FiniteDuration,
                          filter: T => Boolean,
                          statsd: StatsD,
                          model: Option[KMeansModel],
                          epsilon: Option[Double],
                          startValue: Int = 0): Props =
    Props(new CounterActor[T](name, flushInterval, filter, statsd, model, epsilon, startValue))
}
