// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Actor, Cancellable, Props}
import com.typesafe.scalalogging.LazyLogging
import krampus.actor.StatsD

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

//TODO add test
class CounterActor[T : ClassTag](name: String, flushInterval: FiniteDuration,
                                 filter: T => Boolean,
                                 statsd: StatsD,
                                 startValue: Int = 0) extends Actor with LazyLogging {
  private[this] var counter: Int = startValue

  import scala.concurrent.ExecutionContext.Implicits.global

  case object Flush

  //TODO move this outside the actor
  val task: Option[Cancellable] = Some(context.system.scheduler.schedule(Duration.Zero, flushInterval) {
    self ! Flush
  })

  override def receive: Receive = {
    case Flush =>
      logger.info(s"Current count '$name': $counter")
      counter = startValue

    case msg : T if filter(msg) =>
      counter = counter + 1
      statsd.increment(name)

    case x =>
      logger.error(s"Unexpected message: $x")
  }

  override def postStop(): Unit = {
    task.map(_.cancel())
    super.postStop()
  }
}

//TODO move to commons.actor (metrics and score are using the same actors)
object CounterActor {
  def props[T : ClassTag](name: String, flushInterval: FiniteDuration,
                          filter: T => Boolean,
                          statsd: StatsD,
                          startValue: Int = 0): Props =
    Props(new CounterActor[T](name, flushInterval, filter, statsd, startValue))
}
