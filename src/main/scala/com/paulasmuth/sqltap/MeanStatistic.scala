// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.concurrent.atomic.{AtomicInteger}
import java.text.{DecimalFormat}

class MeanStatistic extends Statistic {

  private val sum = new AtomicInteger()
  private val count = new AtomicInteger()
  private var value : Double = 0.0
  private val format = new DecimalFormat("0.00")

  def incr(delta: Double) : Unit= {
    sum.getAndAdd(delta.toInt * 100)
    count.incrementAndGet()
  }

  def decr(delta: Double) : Unit = {
    sum.getAndAdd(delta.toInt * 100)
    count.decrementAndGet()
  }

  def get() : String = {
    format.format(value)
  }

  def flush(f: Double) : Unit = {
    val c = count.get()

    if (c == 0)
      value = 0.0
    else
      value = sum.get() / c / 100.0


    count.set(0)
    sum.set(0)
  }

}
