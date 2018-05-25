// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}
import com.paulasmuth.sqltap.mysql.{SQLQuery}

class RecordLookupJob(worker: Worker, resource: ResourceManifest) extends ReadyCallback[SQLQuery] {

  private val callbacks = new ListBuffer[ReadyCallback[Record]]()

  def execute(record_id: Long) : Unit = {
    if (callbacks.length == 0) {
      return // RecordLookupJob is a noop without callbacks
    }

    val qry = new SQLQuery(
      SQLBuilder.select(resource, resource.id_field, record_id,
        resource.field_names, null, null, null, null))

    qry.attach(this)
    worker.sql_pool.execute(qry)
  }

  def attach(callback: ReadyCallback[Record]) = {
    callbacks += callback
  }

  def ready(qry: SQLQuery) : Unit = {
    if (qry.rows.length == 0)
      return

    val record = new Record(resource)
    record.load(qry.columns, qry.rows.head)

    for (callback <- callbacks) {
      callback.ready(record)
    }
  }

  def error(qry: SQLQuery, err: Throwable) : Unit = {
    Logger.exception(err, false)
  }

}
