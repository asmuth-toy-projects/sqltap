// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.concurrent.atomic.{AtomicInteger}

class IntegralStatistic extends Statistic {

  private val value = new AtomicInteger()

  def incr(delta: Double) : Unit= {
    value.getAndAdd(delta.toInt)
  }

  def decr(delta: Double) : Unit = {
    value.getAndAdd(delta.toInt * -1)
  }

  def get() : String = {
    value.get().toString
  }

  def flush(f: Double) : Unit = {
    // do nothing
  }

}
