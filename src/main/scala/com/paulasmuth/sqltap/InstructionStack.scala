// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class InstructionStack {

  val root   : Instruction = null
  var head   : Instruction = null
  var length : Int         = 0

  def push_down(ins: Instruction) : Unit = {
    ins.prev = head
    head = ins
    ins.prepare()
    length += 1
  }

  def push_field(field: String) = {
    if (field == "*") {
      head.fields.clear()
      head.fields ++= head.relation.resource.field_names
    } else if (!head.has_field(field)) {
      head.fields += field
    }
  }

  def pop() : Unit = {
    if (head.prev == null)
      return

    val ins = head
    head = head.prev
    head.next += ins
    length -= 1
  }

  def inspect() : Unit =
    inspect_one(head, 1)

  private def inspect_one(cur: Instruction, lvl: Int) : Unit = {
    cur.inspect(lvl)

    for (next <- cur.next)
      inspect_one(next, lvl+1)
  }

}
