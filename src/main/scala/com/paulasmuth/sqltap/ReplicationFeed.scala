// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql._
import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider

/**
 * The ReplicationFeed thread is responsible only for a single SQL Connection
 * that pulls the binlog event stream from MySQL.
 */
object ReplicationFeed extends Worker with AbstractSQLConnectionPool {
  //val loop = SelectorProvider.provider().openSelector()
  private val query     = new SQLQuery("SHOW BINARY LOGS;")
  //private val worker    = new Worker()

  Logger.log("replication feed starting...")

  def binlog(event: BinlogEvent) : Unit = event match {
    case evt: UpdateRowsBinlogEvent => {
      if (Manifest.has_table(evt.table_name)) {
        Logger.debug("[Expire] table: " + evt.table_name + ", id: " + evt.primary_key)

        CTreeCache.expire(this,
          Manifest.resource_name_for_table(evt.table_name),
          evt.primary_key.toLong)
      }
    }

    case _ => ()
  }

  def ready(conn: SQLConnection) : Unit = {
    if (query.running) {
      conn.execute(query)
    } else {
      val row = query.rows.last
      val position = row.last.toInt
      val filename = row.head

      conn.start_binlog(filename, position)
    }
  }

  def close(conn: SQLConnection) : Unit = {
    throw new Exception("SQL connection closed")
  }

  def busy(conn: SQLConnection) : Unit = ()

  override def run : Unit = try {
    val conn = new SQLConnection(this)
    conn.configure(Config.get() + (('binlog, "enable")))
    conn.connect()
    super.run()
  } catch {
    case e: Exception => {
      Logger.error("[SQL] [Replication] exception: " + e.toString, false)
      Logger.exception(e, true)
      //conn.close(e) // FIXPAUL: reconnect
    }
  }

}
