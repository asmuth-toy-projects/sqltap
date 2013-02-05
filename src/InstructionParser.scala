package com.paulasmuth.sqltap

// Internal Instructions:
//   -> findSingle(res, id, condition, order, fields...)
//   -> findMulti(res, condition, order, limit, offset, fields...)

object InstructionParser {

  def parse(cur: Instruction) : Unit = cur.name match {

    case "findOne" => {
      cur.name = "findSingle"

      if(cur.args.size == 1)
        cur.args += null

      cur.args += null // condition
      cur.args += null // order
    }

    case "findAll" => {
      cur.name = "findMulti"

      cur.args += null // condition
      cur.args += null // order
      cur.args += null // limit
      cur.args += null // offset
    }

    case "countAll" => {
      cur.name = "findMulti"

      cur.args += null // condition
      cur.args += null // order
      cur.args += null // limit
      cur.args += null // offset

      cur.args += "COUNT"
    }

    case "fetch" => {
      cur.prev.next = cur.prev.next diff List(cur)
      cur.prev.args = cur.prev.args :+ cur.args.head
    }

    case "findSingle" => ()
    case "findMulti" => ()
    case "execute" => ()

    case _ =>
      throw new ParseException("invalid instruction: " + cur.name)

  }

}
