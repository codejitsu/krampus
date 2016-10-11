// Copyright (C) 2016, codejitsu.

package krampus.score.actor

sealed trait Protocol

case object InitializeListener extends Protocol
case object ListenerInitialized extends Protocol
case object Flush extends Protocol
