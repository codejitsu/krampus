// Copyright (C) 2016, codejitsu.

package krampus.queue

final case class RawKafkaMessage(key: Array[Byte], msg: Array[Byte])