// Copyright (C) 2016, codejitsu.

package krampus.actor

import akka.actor.{Actor, ActorLogging, Cancellable, Props}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

//TODO add test
class CounterActor[T : ClassTag](name: String, flushInterval: FiniteDuration,
                                 filter: T => Boolean,
                                 statsd: StatsD,
                                 onFlush: Int => Unit,
                                 startValue: Int = 0) extends Actor with ActorLogging {
  private[this] var counter: Int = startValue

  import scala.concurrent.ExecutionContext.Implicits.global

  case object Flush

  //TODO move this outside the actor
  val task: Option[Cancellable] = Some(context.system.scheduler.schedule(Duration.Zero, flushInterval) {
    self ! Flush
  })

  override def receive: Receive = {
    case Flush =>
      log.info(s"Current count '$name': $counter")

      try { onFlush(counter) }
      catch {
        case NonFatal(t) => log.error(t, "Error by 'onFlush'.")
      }

      counter = startValue

    case msg : T if filter(msg) =>
      counter = counter + 1
      statsd.increment(name)

    case x =>
      log.error(s"Unexpected message: $x")
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
                          onFlush: Int => Unit,
                          startValue: Int = 0): Props =
    Props(new CounterActor[T](name, flushInterval, filter, statsd, onFlush, startValue))
}
