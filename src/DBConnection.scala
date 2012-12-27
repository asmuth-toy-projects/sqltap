package com.paulasmuth.sqltap

import java.sql.ResultSet;
import scala.collection.mutable.LinkedList;

class DBConnection(db_addr: String) {

  SQLTap.log_debug("Connect: " + db_addr)

  var conn : java.sql.Connection = null
  var stmt : java.sql.Statement = null

  connect

  def execute(qry: String) : DBResult = try {
    val strt = System.nanoTime()
    val rslt = execute_without_stopwatch(qry)
    rslt.qtime = System.nanoTime() - strt
    rslt
  } catch {
    case e: Exception => error(
      new Exception(e.toString + "(" + qry + ")"))
  }

  private def execute_without_stopwatch(qry: String) : DBResult = {
    val rslt = stmt.executeQuery(qry)
    val meta = rslt.getMetaData()
    val enum = 1 to meta.getColumnCount()
    var data = new LinkedList[List[String]]()

    val head = (List[String]() /: enum) (
      _ :+ meta.getColumnName(_))

    while (rslt.next)
      data = (List[String]() /: enum) (
        _ :+ rslt.getString(_)) +: data

    new DBResult(head, data)
  }

  private def error(e: Exception) : DBResult = {
    close; connect
    val rslt = new DBResult(null, null)
    rslt.error = e.toString
    rslt
  }

  private def connect : Unit = {
    conn = java.sql.DriverManager.getConnection("jdbc:" + db_addr)
    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
  }

  private def close : Unit = try {
    stmt.close
    conn.close
  } catch {
    case e: Exception => ()
  }

}
