// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object BinaryInteger {

  def read(data: Array[Byte], pos: Int, len: Int) : Int = {
    var value : Int = 0

    for (n <- (0 until len))
      value += (data(pos + n) & 0x000000ff) << (8*n)

    return value
  }

}
