// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.ListBuffer

trait Instruction {

  var next          = ListBuffer[Instruction]()
  var prev          : Instruction = null

  var worker        : Worker = null

  var fields        = ListBuffer[String]()
  var relation      : ResourceRelation = null
  var record        : Record = null
  var args          : ListBuffer[String] = null

  var resource_name : String = null
  var record_id     : String = null

  var running  = false
  var finished = false
  var latch = 0

  val name : String
  def execute(worker: Worker) : Unit

  def prepare() : Unit = {
    if (prev == null || prev.name == "root")
      relation = Manifest.resource(resource_name).to_relation
    else
      relation = prev.relation.resource.relation(resource_name)

    if (relation != null)
      record = new Record(relation.resource)
    else
      throw new ExecutionException("relation not found: " + resource_name)

    fields += relation.resource.id_field
  }

  def execute_next(worker: Worker) : Unit = {
    for (ins <- next)
      if (ins.running == false)
        ins.execute(worker: Worker)

    CTreeCache.flush(worker)
  }

  def unroll() : Unit = {
    latch += 1

    if (latch != 1 + next.length)
      return

    if (prev != null)
      ready()
  }

  def ready() : Unit = {
    prev.unroll()
  }

  def cancel(worker: Worker) : Unit = {
    finished = true
    unroll()
    execute_next(worker)
  }

  def inspect(lvl: Int = 0) : Unit = {
    Logger.debug((" " * (lvl*2)) + "> name: " + name + ", args: [" +
      (if (args != null && args.size > 0) args.mkString(", ") else "none") + "]")

    for (ins <- next)
      ins.inspect(lvl+1)
  }

  def has_field(field: String) : Boolean = {
    for (other <- fields) {
      if (other.equals(field))
        return true
    }

    false
  }

  def meta() : List[(String,String)] = {
    if (relation == null) {
      List(
        (("__resource", "undefined"))
      )
    } else {
      List(
        (("__resource", relation.resource.name))
      )
    }
  }

  def compare(other: Instruction) : Boolean = {
    if (resource_name != other.resource_name || name != other.name)
      return false

    this match {
      case multi: FindMultiInstruction => {
        other match {
          case omulti: FindMultiInstruction => {
            if (multi.conditions != omulti.conditions)
              return false

            if (multi.limit != omulti.limit)
              return false

            if (multi.offset != omulti.offset)
              return false
          }
          case _ => {
            return false
          }
        }
      }

      case count: CountInstruction => {
        other match {
          case ocount: CountInstruction => {
            if (count.conditions != ocount.conditions)
              return false
          }
          case _ => {
            return false
          }
        }
      }

      case _ => ()
    }

    true
  }

  def is_finished() : Boolean = {
    finished
  }

}
