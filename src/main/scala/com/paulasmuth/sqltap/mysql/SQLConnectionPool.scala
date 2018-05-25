// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import com.paulasmuth.sqltap.{Logger,TemporaryException,Statistics}
import java.nio.channels.{Selector}
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap

class SQLConnectionPool(config: Map[Symbol,String], _loop: Selector) extends AbstractSQLConnectionPool {

  val loop : Selector = _loop

  var max_connections =
    config.getOrElse('sql_max_connections, "1").toInt

  var max_queue_len =
    config.getOrElse('sql_queue_max_len, "10").toInt

  private val connections      = new ListBuffer[SQLConnection]()
  private val connections_idle = new ListBuffer[SQLConnection]()
  private val queue            = new ListBuffer[SQLQuery]()

  for (n <- (0 until max_connections))
    connect()

  def execute(query: SQLQuery) : Unit = {
    val connection = get

    if (connection == null) {
      if (queue.length >= max_queue_len)
        throw new TemporaryException("sql queue is full")

      query +=: queue
    } else {
      connection.execute(query)
    }
  }

  def ready(connection: SQLConnection) : Unit = {
    connections_idle += connection

    val pending = math.min(connections_idle.length, queue.length)

    for (n <- (0 until pending)) {
      val conn = get()

      if (conn != null)
        conn.execute(queue.remove(0))
    }

    Statistics.incr('sql_requests_total)
    Statistics.incr('sql_requests_per_second)
  }

  def busy(connection: SQLConnection) : Unit = {
    connections_idle -= connection
  }

  def busy() : Boolean = {
    queue.length >= max_queue_len
  }

  def close(connection: SQLConnection) : Unit = {
    connections      -= connection
    connections_idle -= connection
  }

  def binlog(event: BinlogEvent) : Unit = {
    // do nothing
  }

  private def get() : SQLConnection = {
    if (connections.length < max_connections)
      connect()

    if (connections_idle.length > 0)
      return connections_idle.remove(0)

    return null
  }

  private def connect() : Unit = {
    val conn = new SQLConnection(this)
    conn.configure(config)
    conn.connect()
    connections += conn
  }

}

