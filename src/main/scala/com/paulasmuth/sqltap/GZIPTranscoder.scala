// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{ByteArrayInputStream,ByteArrayOutputStream}
import java.nio.{ByteBuffer}

class GZIPTranscoder(buffer: ElasticBuffer) {

  def encode() : Unit = {
    val buf    = buffer.retrieve()
    val target = new ByteArrayOutputStream(buf.position)
    val gzip   = new GZIPOutputStream(target)

    gzip.write(buf.array, 0, buf.position)

    gzip.finish
    gzip.close
    target.close

    buf.clear()
    buf.put(target.toByteArray())
  }

  def decode() : Unit = {
    val buf    = buffer.retrieve()
    val source = new ByteArrayInputStream(buf.array.clone())
    val gzip   = new GZIPInputStream(source)

    var reading = true
    buf.limit(0)

    while (reading) {
      val pos = buf.limit
      val len = gzip.read(buf.array, pos, buf.capacity - pos)

      if (len > 0) {
        buf.limit(pos + len)
      } else {
        reading = false
      }
    }

    gzip.close
    source.close
  }
}


