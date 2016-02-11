// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

import akka.actor.{Props, Cancellable, Actor}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

class CounterActor[T : ClassTag](name: String, flushInterval: FiniteDuration, filter: T => Boolean, startValue: Int = 0) extends Actor with LazyLogging {
  private[this] var counter: Int = startValue

  import scala.concurrent.ExecutionContext.Implicits.global

  val task: Option[Cancellable] = Some(context.system.scheduler.schedule(Duration.Zero, flushInterval) {
    self ! Flush
  })

  override def receive: Receive = {
    case Flush =>
      logger.info(s"Current count '$name': $counter")
      counter = startValue

    case msg : T if filter(msg) =>
      counter = counter + 1

    case x =>
      logger.error(s"Unexpected message: $x")
  }

  override def postStop(): Unit = {
    task.map(_.cancel())
    super.postStop()
  }
}

object CounterActor {
  def props[T : ClassTag](name: String, flushInterval: FiniteDuration, filter: T => Boolean, startValue: Int = 0): Props =
    Props(new CounterActor[T](name, flushInterval, filter, startValue))
}
