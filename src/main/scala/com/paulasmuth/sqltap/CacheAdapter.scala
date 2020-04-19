// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class CacheAdapter(backend: CacheBackend) {

  val queue = ListBuffer[CacheRequest]()

  // enqueue a request to be executed (doesnt execute yet)
  def enqueue(request: CacheRequest) = {
    request +=: queue
  }

  // executes all enqueue requests (to allow batching)
  def flush() : Unit = {
    if (queue.length == 0)
      return

    val next = queue.toList
    queue.clear()
    backend.execute(next)
  }

  // purges all keys
  def purge(keys: List[String]) : Unit = {
    for (key <- keys) {
      enqueue(new CachePurgeRequest(key))
    }

    flush()
  }

}

