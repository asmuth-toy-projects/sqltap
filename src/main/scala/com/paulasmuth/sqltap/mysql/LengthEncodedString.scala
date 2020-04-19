// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object LengthEncodedString {

  def read(data: Array[Byte], pos: Int) : (String, Int) = {
    var length : Int = 0
    var offset : Int = pos

    if ((data(pos) & 0x000000ff) < 0xfb) {
      length += (data(pos) & 0x000000ff)
      offset += 1
    }

    else if ((data(pos) & 0x000000ff) == 0xfc) {
      length += (data(pos + 1) & 0x000000ff)
      length += (data(pos + 2) & 0x000000ff) << 8
      offset += 3
    }

    else if ((data(0) & 0x000000ff) == 0xfd) {
      length += (data(pos + 1) & 0x000000ff)
      length += (data(pos + 2) & 0x000000ff) << 8
      length += (data(pos + 3) & 0x000000ff) << 16
      offset += 4
    }

    else if ((data(0) & 0x000000ff) == 0xfe && data.size == 9)
      throw new SQLProtocolError("length encoded string too large!")

    else
      throw new SQLProtocolError("invalid length encoded string")

    val string = new String(data, offset, length, "UTF-8")

    return (string, offset + length)
  }

}
