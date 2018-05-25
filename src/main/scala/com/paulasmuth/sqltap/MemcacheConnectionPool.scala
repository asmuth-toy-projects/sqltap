// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class MemcacheConnectionPool extends CacheBackend {

  val MEMCACHE_BATCH_SIZE = 10

  var max_connections =
    Config.get('memcache_max_connections).toInt

  var max_queue_len =
    Config.get('memcache_queue_max_len).toInt

  private val connections      = new ListBuffer[MemcacheConnection]()
  private val connections_idle = new ListBuffer[MemcacheConnection]()
  private val queue            = new ListBuffer[CacheRequest]()
  private val get_queue        = new ListBuffer[CacheGetRequest]()

  def execute(requests: List[CacheRequest]) : Unit = {
    if (queue.length >= max_queue_len) {
      requests.foreach(_.ready())

      Logger.exception(new TemporaryException("memcache queue is full"), false)

      return
    }

    for (request <- requests) {
      request match {
        case get: CacheGetRequest => {
          get +=: get_queue
        }
        case _ => {
          request +=: queue
        }
      }
    }

    execute_next()
  }

  def ready(connection: MemcacheConnection) : Unit = {
    connections_idle += connection

    execute_next()

    Statistics.incr('memcache_requests_total)
    Statistics.incr('memcache_requests_per_second)
  }

  def close(connection: MemcacheConnection) : Unit = {
    connections      -= connection
    connections_idle -= connection
  }

  private def execute_next() : Unit = {
    if (get_queue.length == 0 && queue.length == 0) {
      return
    }

    val conn = get()

    if (conn == null) {
      return
    }

    if (get_queue.length > 0) {
      execute_batch_get(conn)
    } else if (queue.length > 0) {
      execute(conn, queue.remove(0))
      execute_next()
    }
  }

  private def execute_batch_get(conn: MemcacheConnection) : Unit = {
    val batch = new ListBuffer[CacheGetRequest]()
    val keys  = new ListBuffer[String]()

    while (get_queue.length > 0 && batch.length < MEMCACHE_BATCH_SIZE) {
      val req = get_queue.remove(0)
      keys  += req.key
      batch += req
    }

    conn.execute_mget(keys.toList, batch.toList)

    execute_next()
  }

  private def get() : MemcacheConnection = {
    if (connections.length < max_connections)
      connect()

    if (connections_idle.length > 0)
      return connections_idle.remove(0)

    return null
  }

  private def connect() : Unit = {
    val port: Int = Config.get('memcache_port).toInt
    val host: String = Config.get('memcache_host)
    val conn = new MemcacheConnection(this, host, port)

    conn.connect()
    connections += conn
  }

  private def execute(connection: MemcacheConnection, req: CacheRequest) = {
    req match {
      case set: CacheStoreRequest => connection.execute_set(req.key, set)
      case purge: CachePurgeRequest => connection.execute_delete(purge.key)
    }
  }
}
