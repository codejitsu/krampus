// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

sealed trait Protocol

case object StartListener extends Protocol
case object InitializeListener extends Protocol
case object ListenerInitialized extends Protocol
case object Flush extends Protocol
