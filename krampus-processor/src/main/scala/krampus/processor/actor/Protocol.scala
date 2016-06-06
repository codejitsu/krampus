// Copyright (C) 2016, codejitsu.

package krampus.processor.actor

import krampus.entity.WikiChangeEntry

sealed trait Protocol

case object StartStreamProcessor extends Protocol
case object InitializeQueueListener extends Protocol
case object QueueListenerInitialized extends Protocol
final case class MessageConverted(msg: WikiChangeEntry) extends Protocol

final case class Insert(msg: WikiChangeEntry) extends Protocol
