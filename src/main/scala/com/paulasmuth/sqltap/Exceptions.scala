// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class ParseException(msg: String) extends Exception {
  override def toString = msg
}

class ExecutionException(msg: String) extends Exception {
  override def toString = msg
}

class TemporaryException(msg: String) extends Exception {
  override def toString = msg
}

class TimeoutException(msg: String) extends Exception {
  override def toString = msg
}

class NotFoundException(cur: Instruction = null) extends Exception {
  override def toString =
    if (cur == null)
      "not found"
    else
      "could not find record '" +
      (if (cur.relation == null) "null" else cur.relation.name) +
      (if (cur.record.has_id) "' with id #" + cur.record.id.toString else "")
}
