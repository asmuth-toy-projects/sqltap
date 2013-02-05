package com.paulasmuth.sqltap

import scala.collection.mutable.ListBuffer;

class RequestExecutor extends RequestVisitor {

  val SEARCH_DEPTH = 50 // max stack search depth

  var stack = ListBuffer[Instruction]()
  var stime : Long = 0

  def run() : Unit = {
    stime = System.nanoTime
    stack += req.stack.root
    next
  }

  private def next() : Unit = {
    val search_depth =
      Math.min(SEARCH_DEPTH, stack.length - 1)

    for (idx <- (0 to search_depth).reverse)
      if (stack(idx).running == false) {
        execute(stack(idx))

        if (stack(idx).running) 
          return next
      }

    pop(stack.remove(0))

    if (stack.length > 0)
      next
  }

  private def pop(cur: Instruction) : Unit = {
    if (cur.ready)
      return

    if (cur.job == null)
      throw new ExecutionException("deadlock while executing: " + 
        cur.name + ", " + cur.args.mkString(","))

    if (cur.job.retrieve.error != null)
      throw new ExecutionException(cur.job.retrieve.error)

    cur.ready = true

    if (SQLTap.debug) {
      val qtime = (cur.job.result.qtime / 1000000.0).toString
      val otime = ((System.nanoTime - stime) / 1000000.0).toString
      SQLTap.log_debug("Finished (" + qtime + "ms) @ " + otime + "ms: "  + cur.job.query)
    }

    fetch(cur)
  }

  private def execute(cur: Instruction) : Unit =

    if (cur.name == "execute") {
      cur.ready = true

      for (next <- cur.next)
        execute(next)
    }

    else {
      if (cur.relation == null) {
        prepare(cur)

        stack += cur

        if (cur.name != "findMulti")
          for (nxt <- cur.next)
            execute(nxt)

      }

      peek(cur)
    }



  private def prepare(cur: Instruction) = {

    if (cur.prev == req.stack.root) {
      cur.relation = SQLTap.manifest(cur.args(0)).to_relation
    } else {
      cur.relation = cur.prev.relation.resource.relation(cur.args(0))
    }

    if (cur.relation != null)
      cur.prepare
    else
      throw new ExecutionException("relation not found: " + cur.args(0))

    if(cur.name == "findSingle" && cur.args(1) != null)
      cur.record.set_id(cur.args(1).toInt)

  }


  private def fetch(cur: Instruction) : Unit = {

    if (cur.name == "findSingle")
      if (cur.job.retrieve.data.length == 0)
        throw new NotFoundException(cur)
      else
        cur.record.load(cur.job.retrieve.head, cur.job.retrieve.data.head)

    else if (cur.name == "findMulti")
      if (cur.job.retrieve.data.length == 0)
        cur.next = List[Instruction]()

      else {
        val skip_execution = (cur.next.length == 0)
        InstructionFactory.expand(cur)

        if (skip_execution)
          return

        for (ins <- cur.next)
          stack += ins
      }

  }

  private def peek(cur: Instruction) : Unit = cur.name match {

    case "findSingle" => {
      var join_field : String = null

      if (cur.args.size < 5)
        throw new ParseException("empty field list")

      if (cur.record.has_id) {
        join_field = cur.relation.resource.id_field
      }

      else if (cur.relation.join_foreign == false && cur.prev.ready) {
        cur.record.set_id(cur.prev.record.get(cur.relation.join_field).toInt)
        join_field = cur.relation.resource.id_field
      }

      else if (cur.relation.join_foreign == true && cur.prev.record.has_id) {
        cur.record.set_id(cur.prev.record.id)
        join_field = cur.relation.join_field
      }

      if (join_field != null) {
        cur.running = true
        cur.job = SQLTap.db_pool.execute(
          SQLBuilder.sql(
            cur.relation.resource,
            join_field, 
            cur.record.id.toString,
            cur.args.slice(4, cur.args.size).toList, // fields
            cur.args(2), // cond
            cur.args(3), // order
            null, // limit
            null  // offset
          ))
      }
    }

    case "findMulti" => {

      if (cur.args.size < 6)
        throw new ParseException("empty field list")

      if (cur.prev == req.stack.root) {
        cur.running = true
        cur.job = SQLTap.db_pool.execute(
          SQLBuilder.sql(cur.relation.resource, null, null,
            cur.args.slice(5, cur.args.size).toList,
            cur.args(1), cur.args(2), cur.args(3), cur.args(4)))
      }

      else if (cur.relation.join_foreign == true && cur.prev.record.has_id) {
        cur.record.set_id(cur.prev.record.id)
        cur.running = true

        if (cur.args(1) == null && cur.relation.join_cond != null)
          cur.args(1) = cur.relation.join_cond

        cur.job = SQLTap.db_pool.execute(
          SQLBuilder.sql(cur.relation.resource,
            cur.relation.join_field, cur.record.id.toString,
            cur.args.slice(5, cur.args.size).toList,
            cur.args(1), cur.args(2), cur.args(3), cur.args(4)))
      }

      else if (cur.relation.join_foreign == false)
        throw new ParseException("findSome on a non-foreign relation")

    }

  }
}
