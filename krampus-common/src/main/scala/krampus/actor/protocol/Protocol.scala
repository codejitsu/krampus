// Copyright (C) 2016, codejitsu.

package krampus.actor.protocol

import akka.actor.ActorRef
import krampus.entity.WikiEdit

import scala.reflect.ClassTag

sealed trait Protocol

case object StartStreamProcessor extends Protocol
case object InitializeQueueListener extends Protocol
case object QueueListenerInitialized extends Protocol
case object StreamProcessorInitialized extends Protocol
final case class MessageConverted(msg: WikiEdit) extends Protocol

final case class Insert(msg: WikiEdit) extends Protocol

final case class Store[E](msg: E, back: ActorRef, caller: Option[ActorRef])(implicit ev: ClassTag[E]) extends Protocol
final case class StoreResult[R](res: R, back: ActorRef, caller: Option[ActorRef])(implicit ev: ClassTag[R]) extends Protocol
final case class Stored[E](msg: E, caller: Option[ActorRef])(implicit ev: ClassTag[E]) extends Protocol
case object InvalidEntityType extends Protocol
case object GetCountInserted extends Protocol
final case class CountInserted(inserted: Long) extends Protocol
