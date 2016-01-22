// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import java.nio.ByteBuffer

class BinlogDumpPacket(
  server_id: Int,
  binlog_file: String,
  binlog_pos: Int
) extends SQLClientIssuedPacket {
  val binlog_file_b = binlog_file.getBytes("UTF-8")

  def write(buf: ByteBuffer) : Unit = {
    buf.put(0x12.toByte)    // COM_BINLOG_DUMP
    buf.putInt(binlog_pos)
    buf.putShort(0)         // no flags
    buf.putInt(server_id)
    buf.put(binlog_file_b)
  }

  def length() : Int = {
    11 + binlog_file_b.length
  }

}
