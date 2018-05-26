// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class Timeout(_ms: Long, callback: TimeoutCallback) extends Comparable[Timeout] {
  val ms = _ms
  var expires : Long = 0

  def compareTo(o: Timeout) : Int = {
    ms.compareTo(o.ms)
  }

  def start() : Unit = {
    set_expire()
    TimeoutScheduler.add(this)
  }

  def fire() : Unit = {
    callback.timeout()
  }

  def cancel() : Unit = {
    TimeoutScheduler.remove(this)
  }

  def reset() : Unit = {
    TimeoutScheduler.remove(this)
    set_expire()
    TimeoutScheduler.add(this)
  }

  private def set_expire() : Unit = {
    expires = (System.nanoTime / 1000000) + ms
  }

}
