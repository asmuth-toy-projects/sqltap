// This file is part of the "SQLTap" project
//   (c) 2014 Paul Asmuth, Google Inc. <asmuth@google.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT
package com.paulasmuth.sqltap

import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer}

class HTTPConnection(sock: SocketChannel, worker: Worker) extends ReadyCallback[Request] with TimeoutCallback {

  private val HTTP_STATE_INIT  = 1
  private val HTTP_STATE_EXEC  = 2
  private val HTTP_STATE_HEAD  = 3
  private val HTTP_STATE_BODY  = 4
  private val HTTP_STATE_WAIT  = 5
  private val HTTP_STATE_CLOSE = 6

  private val buf = ByteBuffer.allocate(4096)
  private val parser = new HTTPParser()
  private var state = HTTP_STATE_INIT
  private var last_event : SelectionKey = null
  private var last_uri : String = "<unknown>" // for debugging only
  private var keepalive : Boolean = false
  private var resp_buf : ByteBuffer = null

  private var stime : Long = 0

  private var timer : Timeout = TimeoutScheduler.schedule(
      Config.get('http_request_timeout).toInt, this)

  private var idle_timer : Timeout = TimeoutScheduler.schedule(
      Config.get('http_idle_timeout).toInt, this)

  private var seq = 0

  idle_timer.start()

  def read(event: SelectionKey) : Unit = {
    var ready = false
    var eof   = false
    var chunk = 0

    try {
      chunk = sock.read(buf)
    } catch {
      case e: java.io.IOException => return close()
    }

    last_event = event

    if (chunk <= 0)
      close()

    try {
      ready = parser.read(buf)
    } catch {
      case e: HTTPParseError => return close()
    }

    if (ready) {
      event.interestOps(0)
      buf.clear
      execute()
    }
  }

  def write(event: SelectionKey) : Unit = {
    var remaining = 0

    try {
      if (state == HTTP_STATE_BODY) {
        sock.write(resp_buf)
        remaining = resp_buf.remaining
      } else {
        sock.write(buf)
        remaining = buf.remaining
      }
    } catch {
      case e: Exception => {
        Logger.error("[HTTP] conn error: " + e.toString, false)
        return close()
      }
    }

    if (remaining == 0) {
      if (state == HTTP_STATE_HEAD && resp_buf != null) {
        buf.clear
        state = HTTP_STATE_BODY
        write(event)
      } else {
        resp_buf = null
        state = HTTP_STATE_WAIT
        finish()
      }
    }
  }

  def error(e: Throwable) : Unit = e match {
    case e: ParseException =>
      http_error(404, e.toString)

    case e: NotFoundException =>
      http_error(404, e.toString)

    case e: ExecutionException =>
      http_error(500, e.toString)

    case e: TemporaryException =>
      http_error(503, e.toString)

    case e: Exception => {
      Logger.error("[HTTP] exception: " + e.toString, false)
      Logger.exception(e, false)
      close()
    }
  }

  def error(request: Request, err: Throwable) : Unit = {
    timer.cancel()
    error(err)
  }

  def timeout() : Unit = {
    if (state == HTTP_STATE_EXEC)
      error(new ExecutionException("execution expired"))
    else
      close()
  }

  def ready(request: Request) : Unit = {
    timer.cancel()

    val http_buf = new HTTPWriter(buf)
    buf.clear

    resp_buf = request.buffer.retrieve()
    resp_buf.flip()

    http_buf.write_status(200)
    http_buf.write_content_length(resp_buf.remaining())
    http_buf.write_default_headers()
    http_buf.finish_headers()
    buf.flip()

    Statistics.incr('http_bytes_per_second,
      buf.limit + resp_buf.limit)

    worker.requests_success.incrementAndGet()
    flush()
  }

  private def http_error(code: Int, message: String) : Unit = {
    val http_buf = new HTTPWriter(buf)
    val json_buf = new JSONWriter(new WrappedBuffer(buf))

    buf.clear
    json_buf.write_error(message)

    val resp_len = buf.position
    buf.clear

    http_buf.write_status(code)
    http_buf.write_content_length(resp_len)
    http_buf.write_default_headers()
    http_buf.finish_headers()
    json_buf.write_error(message)
    buf.flip

    Statistics.incr('http_errors_total)
    Statistics.incr('http_errors_per_second)

    flush()
  }

  private def flush() : Unit = {
    if (state == HTTP_STATE_CLOSE)
      return

    state = HTTP_STATE_HEAD

    if (last_event != null)
      last_event.interestOps(SelectionKey.OP_WRITE)
  }

  private def execute() : Unit = {
    val route = parser.uri_parts

    idle_timer.cancel()
    stime = System.nanoTime
    last_uri = parser.http_uri
    seq += 1

    if (parser.http_version == "1.1")
      if (Config.get('http_keepalive).equals("true"))
        keepalive = true

    if (route.length >= 1 && route.head == "query")
      execute_request(route.tail)

    else if (route.length == 1 && route.head == "ping")
      execute_ping()

    else if (route.length == 1 && route.head == "stats")
      execute_stats()

    else if (route.length > 1 && route.head == "expire")
      execute_expire(route.tail)

    else if (route.length == 1 && route.head == "log_queries") {
      Config.set('log_queries, "true")
      execute_text(200, "sqltap query logging enabled\r\n")
    }

    else if (route.length == 1 && route.head == "no_log_queries") {
      Config.set('log_queries, "false")
      execute_text(200, "sqltap query logging disabled\r\n")
    }

    else
      http_error(404, "not found")

    Statistics.incr('http_requests_total)
    Statistics.incr('http_requests_per_second)
  }

  private def execute_text(code: Integer, text: String) : Unit = {
    val http_buf = new HTTPWriter(buf)
    buf.clear

    val body = text.getBytes

    http_buf.write_status(code)
    http_buf.write_content_length(body.length)
    http_buf.write_default_headers()
    http_buf.finish_headers()

    buf.put(body)
    buf.flip

    worker.requests_success.incrementAndGet()
    flush()
  }

  private def execute_ping() : Unit = {
    execute_text(200, "pong\r\n")
  }

  private def execute_request(params: List[String]) : Unit = {
    if (seq > 1)
      worker.requests_queued.incrementAndGet()

    if (worker.sql_pool.busy())
      return http_error(503, "no sql connection available")

    timer.start()
    val req = new Request(this)

    for (param <- params)
      req.add_param(param)

    state = HTTP_STATE_EXEC
    last_event.interestOps(0)
    req.execute(worker)
  }

  private def execute_stats() : Unit = {
    val http_buf = new HTTPWriter(buf)
    val json_buf = new JSONWriter(new WrappedBuffer(buf))
    val stats = Statistics.get()

    buf.clear
    json_buf.write_map(stats)

    val resp_len = buf.position
    buf.clear

    http_buf.write_status(200)
    http_buf.write_content_length(resp_len)
    http_buf.write_default_headers()
    http_buf.finish_headers()
    json_buf.write_map(stats)

    buf.flip

    worker.requests_success.incrementAndGet()
    flush()
  }

  private def execute_expire(args: List[String]) : Unit = {
    val http_buf = new HTTPWriter(buf)
    buf.clear

    CTreeCache.expire(worker, args(0), args(1).toInt)

    http_buf.write_status(200)
    http_buf.write_content_length(4)
    http_buf.write_default_headers()
    http_buf.finish_headers()

    buf.put("ok\r\n".getBytes)
    buf.flip

    worker.requests_success.incrementAndGet()
    flush()
  }

  def close() : Unit = {
    if (state == HTTP_STATE_CLOSE)
      return

    timer.cancel()
    last_event.cancel()
    sock.close()
    Statistics.decr('http_connections_open)
    state = HTTP_STATE_CLOSE
  }

  def finish() : Unit = {
    val runtime_millis = (System.nanoTime - stime) / 1000000.0
    Statistics.incr('http_request_time_mean, runtime_millis)

    if (Config.has_key('log_slow_queries) &&
        runtime_millis >= Config.get('log_slow_queries).toInt) {
      Logger.log("[HTTP] [Slow Query] (" + runtime_millis + "ms): " + last_uri)
    }

    if (!keepalive)
      return close()

    state = HTTP_STATE_INIT
    keepalive = false
    last_event.interestOps(SelectionKey.OP_READ)
    idle_timer.reset()
    buf.clear()
    parser.reset()
  }

}
