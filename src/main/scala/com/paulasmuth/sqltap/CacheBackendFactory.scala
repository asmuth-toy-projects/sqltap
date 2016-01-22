// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object CacheBackendFactory {
  def get(worker: Worker) : CacheBackend = {
    val name = Config.get('cache_backend)

    val backend = name match {
      case "memcache" => new MemcacheConnectionPool()
      case "noop" => new NoopCacheBackend()
      case _ => throw new ParseException("unknown cache backend: " + name)
    }

    backend.loop = worker.loop
    backend
  }
}
