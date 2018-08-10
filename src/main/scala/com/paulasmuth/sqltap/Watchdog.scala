// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class Watchdog(base: Server) {

  val max_error_rate = 0.3

  val check_every : Long = 5000L // 5000ms
  var last_check  : Long = System.nanoTime

  def run() : Unit = {
    val now = System.nanoTime

    if ((now - last_check) < (check_every * 1000000L))
      return

    last_check = now

    for (worker <- base.workers)
      check(worker)
  }

  def check(worker: Worker) : Unit = {
    val queued  = worker.requests_queued.get()
    val success = worker.requests_success.get()

    if (queued < 5)
      return

    if (success < 1) {
      kill(worker, "no successful request in the last " +
        check_every + "ms, but had " + queued + " queued")
      return
    }

    val error_rate = ((queued - success) / queued)

    if (error_rate > max_error_rate) {
      kill(worker, "error rate too high: " + error_rate +
       " (queued: " + queued + ", successful: " + success +
       "), kill threshold is: " + max_error_rate)

      return
    }

    worker.requests_queued.set(0)
    worker.requests_success.set(0)
  }

  def kill(worker: Worker, reason: String) : Unit = {
    Logger.error("[WATCHDOG] killing worker: " + reason, false)
    worker.kill()
    base.workers -= worker
  }

}
