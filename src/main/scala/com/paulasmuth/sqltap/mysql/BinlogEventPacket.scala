// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object BinlogEventPacket {

  def load(data: Array[Byte], format: FormatDescriptionBinlogEvent) : BinlogEvent = {
    val timestamp  = BinaryInteger.read(data, 1, 4)
    val event_type = data(5)
    val server_id  = BinaryInteger.read(data, 6, 4)
    val event_size = BinaryInteger.read(data, 10, 4)
    val log_pos    = BinaryInteger.read(data, 14, 4)
    val flags      = BinaryInteger.read(data, 18, 2)

    /*if (event_type == 2) {
      var ep = event_size - 1
      while (data(ep) != '\0') { ep -= 1 }
      if ((event_size - ep) > 3) println(new String(data, ep, event_size - ep - 3))
    }*/

    event_type match {
      case 0x0f => new FormatDescriptionBinlogEvent(data, timestamp)
      case 0x13 => new TableMapBinlogEvent(data, timestamp, format)
      case 0x1f => new UpdateRowsBinlogEvent(data, timestamp, format)
      case _    => new UnknownBinlogEvent(data, timestamp)
    }
  }

}
