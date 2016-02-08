// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

sealed trait Protocol

case object InitReader extends Protocol
case object InitializeReader extends Protocol
case object ReaderInitialized extends Protocol
case object MessageConverted extends Protocol
