// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

object CTreeMarshal {

  private val COUNT_KEY = 1337

  def serialize(buf: CTreeBuffer, cins: Instruction, qins: Instruction) : Unit = {
    qins match {

      case qins_m: CountInstruction => {
        buf.write_header(qins.resource_name)
        buf.write_field(COUNT_KEY, qins.record.get("__count"))
        buf.write_end()
      }

      case qins_m: FindSingleInstruction => {
        buf.write_header(qins.resource_name)

        for (field <- cins.fields) {
          if (qins.record.has_field(field)) {
            val key = qins.relation.resource.field_to_id(field)
            buf.write_field(key, qins.record.get(field))
          }
        }

        for (lins <- cins.next) {
          // FIXPAUL: this doesnt terminate when found
          for (rins <- qins.next) {
            if (lins.compare(rins)) {
              serialize(buf, lins, rins)
            }
          }
        }

        buf.write_end()
      }

      case qins_m: FindMultiInstruction => {
        buf.write_phi(cins.resource_name, qins.next.length)

        for (nxt <- qins.next)
          serialize(buf, cins, nxt)
      }

      case qins_m: PhiInstruction => {
        buf.write_header(cins.resource_name)

        for (field <- cins.fields) {
          if (qins.record.has_field(field)) {
            val key = qins.relation.resource.field_to_id(field)
            buf.write_field(key, qins.record.get(field))
          }
        }

        for (lins <- cins.next) {
          // FIXPAUL: this doesnt terminate when found
          for (rins <- qins.next) {
            if (lins.compare(rins)) {
              serialize(buf, lins, rins)
            }
          }
        }

        buf.write_end()
      }

    }
  }

  def load(buf: CTreeBuffer, ins: Instruction, worker: Worker) : Unit = {
    while (buf.remaining > 0) {
      buf.read_next() match {

        case buf.T_RES => {
          val res_name = buf.read_string()
          var found    = false

          if (ins != null) {
            var n = ins.next.length

            if (ins.resource_name == res_name) {
              found = true
            }

            if (ins.relation.resource.name == res_name) {
              found = true
            }

            if (ins != null) {
              while (!found && n > 0) {
                if (ins.next(n - 1).resource_name == res_name) {
                  found = true
                  load(buf, ins.next(n - 1), worker)
                }

                n -= 1
              }
            }
          }

          if (!found) {
            load(buf, null, worker)
          }
        }

        case buf.T_FLD => {
          val key = buf.read_next()
          var value = buf.read_string()

          if (ins != null) {
            val field = if (key == COUNT_KEY) {
              "__count"
            } else {
              ins.record.resource.id_to_field(key)
            }

            ins.record.set(field, value)
            ins.fields -= field
          }
        }

        case buf.T_END => {
          if (ins == null)
            return

          ins match {

            case ins: PhiInstruction => return

            case ins: FindMultiInstruction => {
              if (ins.fields.length == 0) {
                ins.cancel(worker)
              }

              return
            }

            case ins: FindSingleInstruction => {
              if (ins.fields.length == 0) {
                ins.cancel(worker)
              }

              return
            }

            case ins: CountInstruction => {
              ins.cancel(worker)
              return
            }
          }
        }

        case buf.T_PHI => {
          val len = buf.read_next()
          val res_name = buf.read_string()

          if (ins != null) {
            var nxt : Instruction = null

            if (ins.relation.resource.name.equals(res_name)) {
              nxt = ins
            } else {
              for (_nxt <- ins.next) {
                // FIXPAUL: this doesnt terminate when found
                if (_nxt.resource_name == res_name) {
                  nxt = _nxt
                }
              }
            }

            if (nxt != null) {
              var n = len
              val instructions = new ListBuffer[Instruction]()
              while (n > 0) {
                val nins = new PhiInstruction()
                nins.relation = nxt.relation
                nins.resource_name = nxt.resource_name
                nins.prev = nxt
                nins.record = new Record(nxt.relation.resource)
                InstructionFactory.deep_copy(nxt, nins)
                instructions += nins

                load(buf, nins, worker)

                n -= 1
              }

              nxt.next = instructions

              if (len > 0)
                nxt match {
                  case multi_ins: FindMultiInstruction => {
                    multi_ins.expanded = true

                    for (cfield <- nxt.next.head.record.fields)
                      nxt.fields -= cfield
                  }
                  case _ => ()
                }

              if (nxt.fields.length == 0 || nxt.next.length == 0)
                nxt.cancel(worker)
            }
          }
        }

      }
    }
  }

}
