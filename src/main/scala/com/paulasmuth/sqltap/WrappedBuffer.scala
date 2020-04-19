// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class WrappedBuffer(buf: ByteBuffer) extends AbstractWrappedBuffer {

  def write(data: Array[Byte]) : Unit = {
    buffer.put(data)
  }

  def write(data: Byte) : Unit = {
    buffer.put(data)
  }

  def write(data: Int) : Unit = {
    buffer.putInt(data)
  }

  def retrieve() : ByteBuffer = {
    buffer
  }

  def buffer() : ByteBuffer = {
    buf
  }

  def read_int() : Int = {
    buffer.getInt()
  }

  def read_string(len: Int) : String = {
    val str_buf = new Array[Byte](len)

    buffer.get(str_buf)
    new String(str_buf, "UTF-8")
  }

  def remaining() : Int = {
    buffer.remaining()
  }

}
