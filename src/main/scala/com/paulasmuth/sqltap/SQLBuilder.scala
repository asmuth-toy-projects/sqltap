// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object SQLBuilder {

  def select(
    res: ResourceManifest,
    id_field: String,
    id: Int,
    fields: List[String],
    cond: String,
    order: String,
    limit: String,
    offset: String
  ) : String = (

    "SELECT " +

    fields.map(res.table_name + ".`" + _ + "`").mkString(", ") +

    " FROM " + res.table_name +

    (if (id_field != null || cond != null) " WHERE " else " ") +
    (if (id_field != null) "`" + id_field + "` = " + id.toString else "") +
    (if (cond != null && id_field != null) " AND " else "") +
    (if (cond != null) cond else "") +

    (if (order != null) " ORDER BY " + order else
      if (res.default_order != null) " ORDER BY " + res.default_order
      else "") +

    (if (limit != null) " LIMIT " + limit else "") +
    (if (offset != null) " OFFSET " + offset else "") +

    ";"

  )

  def count(
    res: ResourceManifest,
    id_field: String,
    id: Int,
    cond: String
  ) : String = (

    "SELECT COUNT(*) FROM " + res.table_name +

    (if (id_field != null || cond != null) " WHERE " else " ") +
    (if (id_field != null) "`" + id_field + "` = " + id.toString else "") +
    (if (cond != null && id_field != null) " AND " else "") +
    (if (cond != null) cond else "") +

    ";"

  )

}
