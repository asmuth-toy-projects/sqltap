// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class FormatDescriptionBinlogEvent(data: Array[Byte], ts: Long) extends BinlogEvent {
  val timestamp      = ts
  val binlog_version = BinaryInteger.read(data, 18, 2)
  val mysql_version  = BinaryString.read(data, 20, 50)
  val create_time    = BinaryInteger.read(data, 70, 4)

  if (data(76) != 19) {
    throw new Exception(
      "mysql binlog: event_header_length != 19 in FORMAT_DESCRIPTION_EVENT, " +
      "maybe unsupported mysql version?")
  }

  def header_length(event_type: Int) : Int = {
    data(75 + event_type).toInt
  }
}
