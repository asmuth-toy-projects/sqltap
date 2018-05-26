// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object BinaryString {

  def read(data: Array[Byte], pos: Int, len: Int) : String =
    return new String(data, pos, len, "UTF-8")

  def read_null(data: Array[Byte], pos: Int) : (String, Int) = {
    val max : Int = data.size
    var end : Int = pos

    while (end < max && data(end) != 0)
      end += 1

    val string = new String(data, pos, end - pos, "UTF-8")

    return (string, end + 1)
  }

}
