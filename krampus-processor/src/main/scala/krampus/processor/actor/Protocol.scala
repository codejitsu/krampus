// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

sealed trait Protocol

case object StartListener extends Protocol
case object InitializeListener extends Protocol
case object ListenerInitialized extends Protocol
final case class  MessageConverted[T](msg: T) extends Protocol
case object Flush extends Protocol
