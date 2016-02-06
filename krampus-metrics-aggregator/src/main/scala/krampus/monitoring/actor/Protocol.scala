// Copyright (C) 2016, codejitsu.

package krampus.monitoring.actor

trait Protocol {
  case object InitializeReader
  case object ReaderInitialized
}
