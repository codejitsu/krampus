// Copyright (C) 2016, codejitsu.

package actors

object Messages {
  final case class ChannelMessage(channel: String, json: String)
}
