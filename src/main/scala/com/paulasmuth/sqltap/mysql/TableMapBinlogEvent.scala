// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class TableMapBinlogEvent(data: Array[Byte], ts: Long, fmt: FormatDescriptionBinlogEvent) extends BinlogEvent {
  private var cur      = 0
  private var meta_cur = 0

  val timestamp    = ts
  val table_id     = read_table_id
  val flags        = read_flags
  val schema_name  = read_null_string
  val table_name   = read_null_string
  val column_count = read_column_count
  val column_types = read_column_types
  val column_metas = read_column_metas

  private def read_column_types() : IndexedSeq[Byte] = {
    val types = for (c <- 0 until column_count) yield data(cur + c)
    cur      += column_count
    types
  }

  private def read_column_metas() : IndexedSeq[Int] = {
    column_types.map(read_column_meta(_))
  }

  private def read_column_meta(column_type: Byte) : Int = {
    val x = (column_type & 0x000000ff) match {
      case 0x01 => 0               // 0x01 TINY
      case 0x02 => 0               // 0x02 SHORT
      case 0x03 => 0               // 0x03 LONG
      case 0x04 => read_int(1)     // 0x04 FLOAT
      case 0x05 => read_int(1)     // 0x05 DOUBLE
      case 0x06 => 0               // 0x06 NULL
      case 0x07 => 0               // 0x07 TIMESTAMP
      case 0x08 => 0               // 0x08 LONGLONG
      case 0x09 => 0               // 0x09 INT24
      case 0x0a => 0               // 0x0a DATE
      case 0x0b => 0               // 0x0b TIME
      case 0x0c => 0               // 0x0c DATETIME
      case 0x0d => 0               // 0x0d YEAR
      case 0x0f => read_int(2)     // 0x0f VARCHAR
      case 0x11 => 0               // 0x11 TIMESTAMP2
      case 0x12 => 0               // 0x12 DATETIME2
      case 0xf6 => read_int(2)     // 0xf6 NEWDECIMAL
      case 0xfc => read_int(1)     // 0xfc BLOB
      case c: Int => {
        throw new Exception("unknown mysql column type: " + c.toString)
      }
    }
    x
  }

  private def read_table_id() : Int = {
    if (fmt.header_length(0x13) == 6) {
      cur = 24
      BinaryInteger.read(data, 20, 4)
    } else {
      cur = 26
      BinaryInteger.read(data, 20, 6)
    }
  }

  private def read_flags() : Int = {
    cur += 2
    BinaryInteger.read(data, cur - 2, 2)
  }

  private def read_null_string() : String = {
    val str = BinaryString.read_null(data, cur + 1)
    cur     = str._2
    str._1
  }

  private def read_column_count() : Int = {
    val count = LengthEncodedInteger.read(data, cur)
    cur       = count._2
    meta_cur  = cur + count._1
    count._1
  }

  def read_int(bytes: Int) : Int = {
    val num = BinaryInteger.read(data, cur, bytes)
    cur    += bytes
    num
  }


}
