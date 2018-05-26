// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class CacheStoreRequest(_key: String, _buf: ElasticBuffer, val expire: Int) extends CacheRequest {
  val key : String = _key
  buffer  = _buf

  val gzip_buf  = new GZIPTranscoder(buffer)
  gzip_buf.encode()

  def ready() : Unit = {
    ()
  }

}
