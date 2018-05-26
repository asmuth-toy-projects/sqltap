// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class HTTPWriter(buf: ByteBuffer) {

  val HTTP = "HTTP/1.1 ".getBytes
  val CRLF = "\r\n".getBytes

  def write_status(code: Int) : Unit = {
    buf.put(HTTP)
    buf.put(code.toString.getBytes)
    buf.put(" Fnord".getBytes) // FIXPAUL
    buf.put(CRLF)
  }

  def write_header(key: String, value: String) : Unit = {
    buf.put(key.getBytes)
    buf.put(": ".getBytes)
    buf.put(value.getBytes)
    buf.put(CRLF)
  }

  def write_content_length(len: Int) : Unit = {
    write_header("Content-Length", len.toString)
  }

  def write_default_headers() : Unit = {
    write_header("Server", "SQLTap " + SQLTap.VERSION)
    write_header("Content-Type", "application/json; charset=utf-8")
    write_header("Access-Control-Allow-Origin", "*")
  }

  def finish_headers() : Unit = {
    buf.put(CRLF)
  }

}

