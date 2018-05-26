// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}
import scala.collection.mutable.{ListBuffer}

class Request(callback: ReadyCallback[Request]) extends ReadyCallback[Query] {

  val buffer = new ElasticBuffer(65536)
  val json_stream = new JSONWriter(buffer)
  var latch : Int = 0
  var ttl   : Int = 0
  val queries = new ListBuffer[String]()
  var failed = false
  val stime  = System.nanoTime()

  def execute(worker: Worker) : Unit = try {
    latch = queries.length
    json_stream.write_array_begin()

    for (qry_str <- queries) {
      val query = new Query(qry_str, ttl)
      query.attach(this)
      query.execute(worker)
    }
  } catch {
    case e: Exception => error(e)
  }

  def ready(query: Query) : Unit = try {
    if (failed)
      return

    json_stream.write_query(query)
    latch -= 1

    if (latch > 0) {
      json_stream.write_comma()
    } else {
      json_stream.write_array_end()
      finished()
    }
  } catch {
    case e: Exception => error(e)
  }

  private def finished() : Unit = {
    if (Config.debug)
      Logger.debug("Request finished (" +
        (((System.nanoTime - stime) / 100000) / 10.0) + "ms): ...")

    callback.ready(this)
  }

  def error(query: Query, err: Throwable) : Unit = {
    error(err)
  }

  def error(err: Throwable) : Unit = {
    val exception = new ExecutionException(err.toString)

    // FIXPAUL: kill all running queries
    Logger.exception(err, false)

    if (callback != null)
      callback.error(this, exception)
  }

  def add_param(param: String) = {
    if (param.substring(0, 2) == "q=")
      if (SQLHelper.is_sql(param.substring(2)))
        param.substring(2) +=: queries
      else
        for (qry <- param.substring(2).split(";"))
          qry +=: queries

    else if (param.substring(0, 4) == "ttl=")
      ttl = param.substring(4).toInt

    else if (param.substring(0, 4) == "for=") {
      val qry = queries.remove(0)

      for (rep <- param.substring(4).split(","))
        qry.replace("$", rep) +=: queries
    }

    else
      throw new ParseException("unknown parameter: " + param)
  }

}
