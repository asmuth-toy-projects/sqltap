// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

trait SQLInstruction extends Instruction with ReadyCallback[SQLQuery] {

  def execute_query(worker: Worker, qry_str: String) : Unit = {
    val qry = new SQLQuery(qry_str)
    qry.attach(this)
    worker.sql_pool.execute(qry)
    running = true
  }

  def error(qry: SQLQuery, err: Throwable) = {
    var cur = prev

    while (cur != null) {
      cur match {
        case q: Query => q.error(err)
        case _ => ()
      }
      cur = cur.prev
    }
  }

}
