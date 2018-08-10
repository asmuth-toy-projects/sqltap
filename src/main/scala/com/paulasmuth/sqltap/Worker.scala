// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery,SQLConnectionPool}
import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.{ConcurrentLinkedQueue}
import java.util.concurrent.atomic.{AtomicInteger}

class Worker() extends Thread {

  private val TICK = 50

  @volatile var running = true

  var requests_queued  = new AtomicInteger()
  var requests_success = new AtomicInteger()

  val queue    = new ConcurrentLinkedQueue[SocketChannel]()
  val loop     = SelectorProvider.provider().openSelector()
  val sql_pool = new SQLConnectionPool(Config.get(), loop)
  val cache    = new CacheAdapter(CacheBackendFactory.get(this))

  Logger.log("worker starting...")

  override def run : Unit = while (true) {
    if (!running) {
      Logger.log("worker exiting...")
      return
    }

    loop.select(TICK)

    try {
      TimeoutScheduler.run()
    } catch {
      case e: Exception => {
        Logger.error("exception while running timeouts", false)
        Logger.exception(e, false)
      }
    }

    while (!queue.isEmpty)
      accept()

    val events = loop.selectedKeys().iterator()

    while (events.hasNext) {
      val event = events.next()
      events.remove()

      if (event.isValid) event.attachment match {
        case conn: HTTPConnection => try {

          if (event.isReadable)
            conn.read(event)

          if (event.isValid && event.isWritable)
            conn.write(event)

        } catch {
          case e: Exception => conn.error(e)
        }

        case conn: mysql.SQLConnection => try {

          if (event.isConnectable)
            conn.ready(event)

          if (event.isValid && event.isReadable)
            conn.read(event)

          if (event.isValid && event.isWritable)
            conn.write(event)

        } catch {
          case e: NotFoundException => {
            // but do not log it, as it's a client side error we do not care
            // about in the server side log
            conn.close(e)
          }
          case e: Exception => {
            Logger.error("[SQL] exception: " + e.toString, false)
            Logger.exception(e, false)
            conn.close(e)
          }
        }

        case conn: MemcacheConnection => try {

          if (event.isConnectable)
            conn.ready(event)

          if (event.isValid && event.isReadable)
            conn.read(event)

          if (event.isValid && event.isWritable)
            conn.write(event)

        } catch {
          case e: Exception => {
            Logger.error("[Memcache] exception: " + e.toString, false)
            Logger.exception(e, false)
            conn.close(e)
          }
        }

      }
    }
  }

  private def accept() : Unit = {
    val conn : SocketChannel = queue.poll()

    if (conn == null)
      return

    conn.configureBlocking(false)

    conn
      .register(loop, SelectionKey.OP_READ)
      .attach(new HTTPConnection(conn, this))
  }

  def kill() : Unit = {
    running = false
  }

}
