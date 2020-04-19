// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

trait AbstractWrappedBuffer {
  def write(data: Array[Byte]) : Unit
  def write(data: Byte) : Unit
  def write(data: Int) : Unit
  def retrieve() : ByteBuffer
  def read_int() : Int
  def read_string(len: Int) : String
  def remaining() : Int
}
