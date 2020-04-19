// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object Statistics {

  private val stats = Map[Symbol, Statistic](
    'http_connections_open        -> new IntegralStatistic,
    'http_requests_total          -> new IntegralStatistic,
    'http_requests_per_second     -> new DeltaStatistic,
    'http_bytes_per_second        -> new DeltaStatistic,
    'http_errors_total            -> new IntegralStatistic,
    'http_errors_per_second       -> new DeltaStatistic,
    'http_request_time_mean       -> new MeanStatistic,
    'sql_connections_open         -> new IntegralStatistic,
    'sql_requests_total           -> new IntegralStatistic,
    'sql_requests_per_second      -> new DeltaStatistic,
    'sql_request_time_mean        -> new MeanStatistic,
    'sql_slow_queries_total       -> new IntegralStatistic,
    'sql_slow_queries_per_second  -> new DeltaStatistic,
    'sql_slow_queries_time_mean   -> new MeanStatistic,
    'memcache_requests_total      -> new IntegralStatistic,
    'memcache_requests_per_second -> new DeltaStatistic,
    'memcache_connections_open    -> new IntegralStatistic
  )

  private var last_update = System.nanoTime

  def incr(key: Symbol, value: Double = 1.0) : Unit = {
    stats(key).incr(value)
  }

  def decr(key: Symbol, value: Double = 1.0) : Unit = {
    stats(key).decr(value)
  }

  def get() : Map[String, String] = {
    stats.map { s => ((s._1.name, s._2.get())) }
  }

  def update_async() : Unit = {
    val thread = (new Thread {
      override def run() : Unit = {
        while (true) {
          val time = System.nanoTime - last_update
          stats.foreach(_._2.flush(time / 1000000000.0))
          last_update = System.nanoTime
          Thread.sleep(1000)
        }
      }
    })

    thread.start()
  }

}

