package com.paulasmuth.sqltap

object SQLBuilder {

  def sql(
    res: ResourceManifest,
    id_field: String,
    id: String,
    fields: List[String],
    cond: String,
    order: String,
    limit: String,
    offset: String
  ) : String = (

    "SELECT " +

    (if (fields.size == 1 && fields.head == "*") "*" 
      else if (fields.size==1 && fields.head == "COUNT") "COUNT(*) AS count"
      else fields.map(res.table_name + ".`" + _ + "`").mkString(", ")) +

    " FROM " + res.table_name +

    (if (id_field != null || cond != null) " WHERE " else " ") +
    (if (id_field != null) "`" + id_field + "` = " + id else "") +
    (if (cond != null && id_field != null) " AND " else "") +
    (if (cond != null) cond else "") +

    (if (order != null) " ORDER BY " + order else
      if (res.default_order != null) " ORDER BY " + res.default_order
      else "") +

    (if (limit != null) " LIMIT " + limit else "") +
    (if (offset != null) " OFFSET " + offset else "") +

    ";"

  )

}
