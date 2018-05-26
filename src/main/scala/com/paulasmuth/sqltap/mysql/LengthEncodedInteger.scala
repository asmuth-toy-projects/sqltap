// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object LengthEncodedInteger {

  def read(data: Array[Byte]) : Int = {
    var value : Int = 0

    if ((data(0) & 0x000000ff) < 0xfb) {
      value += (data(0) & 0x000000ff)
    }

    else if ((data(0) & 0x000000ff) == 0xfc && data.size == 3) {
      value += (data(1) & 0x000000ff)
      value += (data(2) & 0x000000ff) << 8
    }

    else if ((data(0) & 0x000000ff) == 0xfd && data.size == 4) {
      value += (data(1) & 0x000000ff)
      value += (data(2) & 0x000000ff) << 8
      value += (data(3) & 0x000000ff) << 16
    }

    else if ((data(0) & 0x000000ff) == 0xfe && data.size == 9)
      throw new SQLProtocolError("length encoded integer too large!")

    else
      throw new SQLProtocolError("invalid length encoded integer")

    return value
  }

  def read(data: Array[Byte], pos: Int) : ((Int, Int)) = {
    var value  : Int = 0
    var length : Int = 0

    if ((data(pos) & 0x000000ff) < 0xfb) {
      length = 1
      value += (data(pos) & 0x000000ff)
    }

    else if ((data(pos) & 0x000000ff) == 0xfc) {
      length = 3
      value += (data(pos + 1) & 0x000000ff)
      value += (data(pos + 2) & 0x000000ff) << 8
    }

    else if ((data(pos) & 0x000000ff) == 0xfd) {
      length = 4
      value += (data(pos + 1) & 0x000000ff)
      value += (data(pos + 2) & 0x000000ff) << 8
      value += (data(pos + 3) & 0x000000ff) << 16
    }

    else if ((data(pos) & 0x000000ff) == 0xfe)
      throw new SQLProtocolError("length encoded integer too large!")

    else
      throw new SQLProtocolError("invalid length encoded integer")

    return ((value, pos + length))
  }

}
