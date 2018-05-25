// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class FindSingleInstruction extends SQLInstruction with CTreeInstruction {

  val INS_STATE_INIT = 1
  val INS_STATE_PREP = 2
  val INS_STATE_READY = 3
  val INS_STATE_CTREE = 4
  val INS_STATE_QREADY = 5
  val INS_STATE_QUERY = 6
  val INS_STATE_DONE = 7

  val name = "findSingle"
  var state = INS_STATE_INIT

  var conditions : String = null
  var order      : String = null

  var join_field  : String  = null
  var join_id     : Long    = 0
  var allow_empty : Boolean = false

  def execute(_worker: Worker) : Unit = {
    worker = _worker

    if (finished)
      state = INS_STATE_DONE

    state match {

      case INS_STATE_INIT => {
        if (record_id != null)
          record.set_id(record_id)

        if (!fields.contains(record.resource.id_field))
          fields += record.resource.id_field

        state = INS_STATE_PREP
        return execute(worker)
      }

      case INS_STATE_PREP => {
        if (record.has_id) {
          state = INS_STATE_READY

          join_field = relation.resource.id_field
          join_id = record.id
        }

        else if (relation.join_foreign == false && prev.is_finished) {
          val join_id_str = prev.record.get(relation.join_field)

          if (join_id_str == null) {
            state = INS_STATE_DONE
            next.clear()
            return cancel(worker)
          }

          state = INS_STATE_READY
          join_id = join_id_str.toLong

          if (join_id == 0) {
            state = INS_STATE_DONE
            next.clear()
            return cancel(worker)
          }

          if (relation.join_field_remote != null) {
            join_field = relation.join_field_remote
          } else {
            join_field = relation.resource.id_field
            record.set_id(join_id)
          }
        }

        else if (relation.join_foreign == true && prev.record.has_id) {
          state = INS_STATE_READY
          join_field = relation.join_field
          join_id = prev.record.id
        }

        if (state == INS_STATE_READY) {
          execute(worker)
        } else if (prev.is_finished) {
          throw new ExecutionException("deadlock detected")
        }
      }

      case INS_STATE_READY => {
        CTreeIndex.find(this) match {
          case None => {
            state = INS_STATE_QREADY
            execute(worker)
          }
          case Some((_ctree, cost)) => {
            state = INS_STATE_CTREE

            ctree      = _ctree
            ctree_cost = cost
            ctree_key  = ctree.key(join_field, join_id.toString, null)

            CTreeCache.retrieve(worker, ctree, ctree_key, this)
            return
          }
        }
      }

      case INS_STATE_QREADY => {
        // optimization: skip select id from ... where id = ...; queries
        if (fields.length == 1 && fields.head == record.resource.id_field) {
          state = INS_STATE_DONE
          return cancel(worker)
        }

        // workaround: always prepend the id field, since mysql will respond with a
        // leading EOF_packet if the first queried field is null
        if (!fields.contains(record.resource.id_field)) {
          record.resource.id_field +=: fields
        }

        state = INS_STATE_QUERY

        execute_query(worker,
          SQLBuilder.select(
            relation.resource, join_field, join_id, fields.toList,
            conditions, order, null, null))
      }

      case INS_STATE_DONE =>
        return

      case INS_STATE_CTREE =>
        return

    }

    if (running == true)
      execute_next(worker)
  }

  def ready(query: SQLQuery) : Unit = {
    state = INS_STATE_DONE

    if (query.rows.length == 0) {
      if (!allow_empty)
        throw new NotFoundException(this)
    } else {
      if (record.fields.length < 2)
        record.load(query.columns, query.rows(0))
      else
        record.update(query.columns, query.rows(0))
    }

    finished = true
    execute_next(worker)

    unroll()
  }

  override def ctree_ready(worker: Worker) : Unit = {
    if (state != INS_STATE_CTREE)
      return

    state = if (finished)
      INS_STATE_DONE
    else
      INS_STATE_QREADY

    super.ctree_ready(worker)
  }

}
