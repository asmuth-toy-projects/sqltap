// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

trait RowsBinlogEvent extends BinlogEvent {
  val data : Array[Byte]
  var cur = 0

  def load_column(col: Int, column_type: Byte, meta: Int) : String = {
    column_type match {
      case 0x01 => read_int(1).toString       // 0x01 TINY
      case 0x02 => read_int(2).toString       // 0x02 SHORT
      case 0x03 => read_int(4).toString       // 0x03 LONG
      case 0x04 => read_float(4).toString     // 0x04 FLOAT
      case 0x05 => read_float(8).toString     // 0x05 DOUBLE
      case 0x06 => null                       // 0x06 NULL
      case 0x07 => read_int(4).toString       // 0x07 TIMESTAMP
      case 0x08 => read_int(8).toString       // 0x08 LONGLONG
      case 0x09 => read_int(3).toString       // 0x09 INT24
      case 0x0a => read_date(3)               // 0x0a DATE
      case 0x0b => read_date(3)               // 0x0b TIME
      case 0x0c => read_date(8)               // 0x0c DATETIME
      case 0x0d => read_date(1)               // 0x0d YEAR
      case 0x0f => read_varchar(meta)         // 0x0f VARCHAR
      case c: Byte => {
        throw new Exception("unknown mysql column type: " + c.toString)
      }
    }
  }

  def read_table_id(fmt: FormatDescriptionBinlogEvent) : Int = {
    if (fmt.header_length(0x1f) == 6) {
      cur = 24
      BinaryInteger.read(data, 20, 4)
    } else {
      cur = 26
      BinaryInteger.read(data, 20, 6)
    }
  }

  def read_flags() : Int = {
    cur += 2
    BinaryInteger.read(data, cur - 2, 2)
  }

  def read_extra_data() : String = {
    val len = BinaryInteger.read(data, cur, 2)
    cur += len

    if (len > 2) {
      BinaryString.read(data, cur + 2 - len, len - 2)
    } else {
      null
    }
  }

  def read_num_cols() : Int = {
    val num = LengthEncodedInteger.read(data, cur)
    cur     = num._2
    num._1
  }

  def read_int(bytes: Int) : Int = {
    val num = BinaryInteger.read(data, cur, bytes)
    cur += bytes
    num
  }

  def read_date(len: Int) : String = {
    //println(java.util.Arrays.copyOfRange(data, cur, cur + 11).toList.map{x=>x&0x000000ff})
    //println("DATE LEN", data(cur - 1) & 0x000000ff)
    //println("DATE LEN", data(cur) & 0x000000ff)
    //println("DATE LEN", data(cur + 1) & 0x000000ff)
    cur    += len
    "date" // FIXPAUL
  }

  def read_varchar(maxlen: Int) : String = {
    var len = data(cur) & 0x000000ff

    if (maxlen > 255) {
      //len += (data(cur) & 0x000000ff) << 8
      cur += 2
    } else {
      cur += 1
    }

    val str = BinaryString.read(data, cur, len + 1)
    cur    += len
    str
  }

  def read_float(len: Int) : Double = {
    val value = read_int(len)
    java.lang.Double.longBitsToDouble(value) // FIXPAUL
  }

  def read_bitmap(len: Int) : Integer = {
    val old = cur
    cur    += (len + 7) / 8
    old
  }

  def bitmap_count(pos: Int, len: Int) : Int = {
    (0 until (len + 7) / 8).foldLeft(0) {
      (m, c) => {
        m + Integer.bitCount(data(pos + c) & 0x000000ff)
      }
    }
  }

  def bitmap_test(pos: Int, bit: Int) : Boolean = {
    val byte = data(pos + (bit / 8)) & 0x000000ff
    (byte & (1 << (bit % 8))) > 0
  }

}
