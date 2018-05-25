// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.{ListBuffer}

class FindMultiInstruction extends SQLInstruction with CTreeInstruction  {

  val INS_STATE_INIT = 1
  val INS_STATE_PREP = 2
  val INS_STATE_READY = 3
  val INS_STATE_CTREE = 4
  val INS_STATE_QREADY = 5
  val INS_STATE_QUERY = 6
  val INS_STATE_DONE = 7

  val name = "findMulti"
  var state = INS_STATE_INIT

  var conditions : String  = null
  var order      : String  = null
  var limit      : String  = null
  var offset     : String  = null
  var expanded   : Boolean = false

  var join_id : Long = 0
  var join_conditions : String = null

  def execute(_worker: Worker) : Unit = {
    worker = _worker

    if (finished)
      state = INS_STATE_DONE

    state match {

      case INS_STATE_INIT => {
        join_conditions = conditions

        if (!has_field(record.resource.id_field))
          fields += record.resource.id_field

        if (relation.join_foreign == true) {
          if (conditions == null) {
            join_conditions = relation.join_cond
          }

          else if (relation.join_cond != null) {
            join_conditions += " AND " + relation.join_cond
          }
        }

        state = INS_STATE_PREP
        execute(worker)
      }

      case INS_STATE_PREP => {
        if (prev.name == "root") {
          state = INS_STATE_QREADY
          return execute(worker)
        }

        if (relation.join_foreign == true) {
          if (relation.join_field_local == null && prev.record.has_id) {
            state = INS_STATE_READY
            join_id = prev.record.id
            prev.record.set_id(join_id)
          }

          else if (relation.join_field_local != null && prev.is_finished) {
            state = INS_STATE_READY
            join_id = prev.record.get(relation.join_field_local).toLong
          }
        }

        else if (relation.join_foreign == false) {
          throw new ParseException("findAll can't be used with a non-foreign relation")
        }

        if (state == INS_STATE_READY) {
          return execute(worker)
        } else if (prev.finished) {
          throw new ExecutionException(
            "deadlock detected (query contains unresolved dependencies), " +
            "resource: " + resource_name)
        }
      }

      case INS_STATE_READY => {
        CTreeIndex.find(this) match {
          case None => {
            state = INS_STATE_QREADY
            execute(worker)
          }

          case Some((_ctree, cost)) => {
            if (!_ctree.allow_conditions) {
              state = INS_STATE_QREADY
              return execute(worker)
            }

            state = INS_STATE_CTREE

            ctree      = _ctree
            ctree_cost = cost
            ctree_key  = ctree.key(relation.join_field,
              join_id.toString, relation.join_cond)

            CTreeCache.retrieve(worker, ctree, ctree_key, this)
            return
          }
        }
      }

      case INS_STATE_QREADY => {
        state = INS_STATE_QUERY

        if (prev.name == "root") {
          execute_query(worker,
            SQLBuilder.select(
              relation.resource, null, 0, fields.toList,
              conditions, order, limit, offset))
        } else {
          execute_query(worker,
            SQLBuilder.select(
                relation.resource, relation.join_field, join_id,
                fields.toList, join_conditions, order, limit, offset))
        }
      }

      case INS_STATE_CTREE =>
        return

      case INS_STATE_DONE =>
        return

      case INS_STATE_QUERY =>
        return

    }

  }

  def ready(query: SQLQuery) : Unit = {
    state = INS_STATE_DONE

    if (expanded)
      load_after_expansion(query)
    else
      load_and_expand(query)

    finished = true

    execute_next(worker)
    unroll()
  }

  private def load_and_expand(query: SQLQuery) = {
    if (query.rows.length == 0)
      next = new ListBuffer[Instruction]()

    val instructions = new ListBuffer[Instruction]()

    for (row <- query.rows) {
      val ins = new PhiInstruction()
      ins.relation = relation
      ins.prev = this
      ins.record = new Record(relation.resource)
      ins.record.load(query.columns, row)
      InstructionFactory.deep_copy(this, ins)
      instructions += ins
    }

    next = instructions
  }

  private def load_after_expansion(query: SQLQuery) = {
    for (row <- query.rows) {
      var n     = next.length
      var found = false

      val this_id = row(
        query.columns.indexOf(relation.resource.id_field)).toLong

      while (!found && n > 0) {
        val this_ins = next(n - 1)

        if (this_ins.record.id == this_id) {
          this_ins.record.update(query.columns, row)
          found = true
        }

        n -= 1
      }
    }
  }

  override def ctree_ready(worker: Worker) : Unit = {
    state = if (finished)
      INS_STATE_DONE
    else
      INS_STATE_QREADY

    super.ctree_ready(worker)
  }

  override def ready() : Unit = {
    if (next.length == 0)
      ctree_store = false

    if (ctree_store)
      CTreeCache.store(worker, ctree, ctree_key, this)

    prev.unroll()
  }

}
