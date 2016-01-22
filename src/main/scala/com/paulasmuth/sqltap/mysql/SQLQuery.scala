// This file is part of the "SQLTap" project
//   (c) 2014 Paul Asmuth, Google Inc. <asmuth@google.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT
package com.paulasmuth.sqltap.mysql

import com.paulasmuth.sqltap._
import scala.collection.mutable.ListBuffer

class SQLQuery(query_str: String) extends TimeoutCallback {
  var running  = true
  val query    : String = query_str
  var columns  = new ListBuffer[String]()
  var rows     = new ListBuffer[ListBuffer[String]]()
  var num_cols : Int  = 0
  var qtime    : Long = 0
  var callback : ReadyCallback[SQLQuery] = null
  var timer    : Timeout = null

  private var tik : Long = 0
  private var tok : Long = 0

  def start() : Unit = {
    tik = System.nanoTime

    Logger.debug("Execute: " + query)

    timer = TimeoutScheduler.schedule(
      Config.get('sql_timeout).toInt, this)

    timer.start()
  }

  def ready() : Unit = {
    timer.cancel()
    running = false

    if (callback != null)
      callback.ready(this)

    tok = System.nanoTime
    qtime = tok - tik

    val runtime_millis = qtime / 1000000.0
    Statistics.incr('sql_request_time_mean, runtime_millis)
    Logger.debug("Finished (" + runtime_millis + "ms): " + query)

    if (Config.has_key('log_slow_queries) &&
        runtime_millis >= Config.get('log_slow_queries).toInt) {
      Statistics.incr('sql_slow_queries_total)
      Statistics.incr('sql_slow_queries_per_second)
      Statistics.incr('sql_slow_queries_time_mean, runtime_millis)

      Logger.log("[SQL] [Slow Query] (" + runtime_millis + "ms): " + query)
    }
  }

  def error(err: Throwable) : Unit = {
    val error = if (err != null) err else
      new ExecutionException("error while executing SQL query")

    if (timer != null)
      timer.cancel()

    if (callback != null)
      callback.error(this, err)
  }

  def timeout() : Unit = {
    // FIXPAUL: this should actually kill the query in mysql
    if (callback != null)
      callback.error(this, new TimeoutException(
        "sql query timed out after " + timer.ms + "ms: " + query))

    callback = null
  }

  def attach(_callback: ReadyCallback[SQLQuery]) : Unit =
    callback = _callback

}
