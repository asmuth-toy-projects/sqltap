// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object ColumnDefinition {

  def read_name(data: Array[Byte]) : String = {
    var cur       : Int           = 0
    var catalog   : (String, Int) = null
    var schema    : (String, Int) = null
    var table     : (String, Int) = null
    var org_table : (String, Int) = null
    var name      : (String, Int) = null
    var org_name  : (String, Int) = null
    var charset   : Int           = 0
    var col_len   : Int           = 0
    var col_type  : Int           = 0
    var flags     : Int           = 0

    catalog   = LengthEncodedString.read(data, cur)
    schema    = LengthEncodedString.read(data, catalog._2)
    table     = LengthEncodedString.read(data, schema._2)
    org_table = LengthEncodedString.read(data, table._2)
    name      = LengthEncodedString.read(data, org_table._2)
    org_name  = LengthEncodedString.read(data, name._2)

    cur      += org_name._2 + 1

    charset   = BinaryInteger.read(data, cur, 2)
    cur      += 2

    col_len   = BinaryInteger.read(data, cur, 4)
    cur      += 4

    col_type  = BinaryInteger.read(data, cur, 1)
    cur      += 1

    flags     = BinaryInteger.read(data, cur, 2)

    return name._1
  }

}
