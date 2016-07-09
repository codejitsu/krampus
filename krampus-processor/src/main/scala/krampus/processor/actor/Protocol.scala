// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import akka.actor.ActorRef
import krampus.entity.WikiChangeEntry

import scala.reflect.ClassTag

sealed trait Protocol

case object StartStreamProcessor extends Protocol
case object InitializeQueueListener extends Protocol
case object QueueListenerInitialized extends Protocol
final case class MessageConverted(msg: WikiChangeEntry) extends Protocol

final case class Insert(msg: WikiChangeEntry) extends Protocol

final case class Store[E](msg: E, back: ActorRef)(implicit ev: ClassTag[E]) extends Protocol
final case class StoreResult[R](res: R, back: ActorRef)(implicit ev: ClassTag[R]) extends Protocol
final case class Stored[E](msg: E)(implicit ev: ClassTag[E]) extends Protocol
case object InvalidEntityType extends Protocol
