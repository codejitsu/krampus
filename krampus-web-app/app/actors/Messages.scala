// Copyright (C) 2017, codejitsu.

package actors

object Messages {
  final case class ChannelMessage(channel: String, json: String)
}
