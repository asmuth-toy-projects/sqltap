// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class CacheGetRequest(_key: String) extends CacheRequest {
  val key : String = _key
  var instruction : CTreeInstruction = null
  var fired = false

  def retrieve() : Option[ElasticBuffer] = {
    if (buffer == null) None else Some(buffer)
  }

  def ready() : Unit = {
    if (fired)
      return

    fired = true

    if (buffer != null) {
      val gzip_buf = new GZIPTranscoder(buffer)
      gzip_buf.decode()

      val ctree_buf = new CTreeBuffer(buffer)
      CTreeMarshal.load(ctree_buf, instruction, worker)

      instruction.ctree_ready(worker)
    }

    instruction.ctree_ready(worker)
  }

}
