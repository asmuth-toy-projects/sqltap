// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql
import scala.collection.mutable.{ListBuffer}

class UpdateRowsBinlogEvent(d: Array[Byte], ts: Long, fmt: FormatDescriptionBinlogEvent) extends RowsBinlogEvent {
  var rows         = new ListBuffer[List[String]]()
  val timestamp    = ts
  val data         = d
  val table_id     = read_table_id(fmt)
  val flags        = read_flags
  val extra_data   = read_extra_data
  val num_cols     = read_num_cols
  val column_map1  = read_bitmap(num_cols)
  val column_map2  = read_bitmap(num_cols)
  var primary_key  = ""
  var table_name   = ""

  def load(table: TableMapBinlogEvent) : Unit = {
    val rows = load_row_part(table, column_map1)

    // FIXPAUL: hack!!!
    table_name  = table.table_name
    primary_key = rows(0)._2
  }

  private def load_row_part(table: TableMapBinlogEvent, colmap: Int) : IndexedSeq[(Int, String)] = {
    val cols_present = bitmap_count(colmap, num_cols)
    val null_bitmap  = read_bitmap(cols_present)
    def test(c: Int) = bitmap_test(colmap, c) && !bitmap_test(null_bitmap, c)
    def load(c: Int) = load_column(c, table.column_types(c), table.column_metas(c))

    for (col <- 0 until 1)
      yield (test(col)) match {
        case true  => ((col, load(col)))
        case false => ((col, null))
      }
  }

}
