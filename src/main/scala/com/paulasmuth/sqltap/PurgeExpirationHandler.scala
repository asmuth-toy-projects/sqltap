// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

/**
 * Expires CTree Cache entries by simply purging them from the cache
 */
class PurgeExpirationHandler extends ExpirationHandler {

  /**
   * Expires a CTree Cache entry by simply purging it from the cache
   *
   * @param worker the current worker thread
   * @param key    the cache key
   */
  def execute(worker: Worker, key: String) : Unit = {
    worker.cache.purge(List(key))
  }

}
