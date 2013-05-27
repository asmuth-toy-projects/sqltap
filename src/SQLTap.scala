// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.Locale
import java.util.Date
import java.text.DateFormat
import java.io.File
import scala.collection.mutable.HashMap;

object SQLTap{

  val VERSION = "v0.2.5"
  val CONFIG  = HashMap[Symbol,String]()

  var DEFAULTS = HashMap[Symbol, String](
    'http_port     -> "8080",
    'memcached_ttl -> "3600"
  )

  val manifest = HashMap[String,ResourceManifest]()
  val prepared_queries = HashMap[String,PreparedQuery]()

  var debug   = false


  def main(args: Array[String]) : Unit = {
    var n = 0

    while (n < args.length) {

      if (args(n) == "--http")
        { CONFIG += (('http_port, args(n+1))); n += 2 }

      else if (args(n) == "--mysql-host")
        { CONFIG += (('mysql_host, args(n+1))); n += 2 }

      else if (args(n) == "--mysql-port")
        { CONFIG += (('mysql_port, args(n+1))); n += 2 }

      else if (args(n) == "--mysql-user")
        { CONFIG += (('mysql_user, args(n+1))); n += 2 }

      else if (args(n) == "--mysql-password")
        { CONFIG += (('mysql_pass, args(n+1))); n += 2 }

      else if (args(n) == "--mysql-database")
        { CONFIG += (('mysql_db, args(n+1))); n += 2 }

      else if (args(n) == "--memcached-ttl")
        { CONFIG += (('memcached_ttl, args(n+1))); n += 2 }

      else if (args(n) == "--memcached")
        { CONFIG += (('memcached, args(n+1))); n += 2 }

      else if ((args(n) == "-c") || (args(n) == "--config"))
        { CONFIG += (('config_base, args(n+1))); n += 2 }

      else if ((args(n) == "-d") || (args(n) == "--debug"))
        { debug = true; n += 1 }

      else if ((args(n) == "-h") || (args(n) == "--help"))
        return usage(true)

      else if ((args(n) == "--preheat") && args.size > n + 1)
        { CONFIG += (('preheat_src, args(n+1))); n += 2 }

      else {
        println("error: invalid option: " + args(n) + "\n")
        return usage(false)
      }

    }

    if (CONFIG contains 'config_base unary_!)
      { log("--config required"); println; return usage(true)  }

    DEFAULTS.foreach(d =>
      if (CONFIG contains d._1 unary_!) CONFIG += d )

    CONFIG.get('preheat_src) match {
      case Some(_) => preheat
      case None => boot
    }
  }

  def boot = try {
    load_config

    val workers = List(new Worker)
    for (worker <- workers) worker.start()

    val acceptor = new Acceptor(workers)
    acceptor.run(CONFIG('http_port).toInt)
  } catch {
    case e: Exception => exception(e, true)
  }

  def preheat = try {
    load_config

    val xml = scala.xml.XML.loadFile(CONFIG('preheat_src))

    val (existing, non_existing) = (xml \ "prepared_query").partition { node =>
      prepared_queries.get((node \ "@name").text).isDefined
    }

    if (existing.nonEmpty) {
      val db_threads = CONFIG('db_threads).toInt
      //db_pool.connect(CONFIG('db_addr), db_threads)

      existing.foreach { node =>
        val query_name = (node \ "@name").text
        val ids = (node \ "id").map(_.text.toInt).toList
        val query = prepared_queries(query_name)

        println("prepared query: " + query_name + "\nnumber of ids: " + ids.length)

       // PreparedQueryCache.execute(query, ids, NullOutputStream, true)
      }

      //PreparedQueryCache.shutdown
      //db_pool.shutdown
    }

    if (non_existing.nonEmpty) {
      println("missing prepared queries")
      non_existing.map( node => ( node \ "@name" ).text).foreach(println)
    }

  } catch {
    case e: Exception => exception(e, true)
  }


  def load_config = {
    SQLTap.log("sqltapd " + VERSION + " booting...")

    val cfg_base = new File(CONFIG('config_base))

    val sources : Array[String] =
      if (cfg_base.isDirectory unary_!)
        Array(io.Source.fromFile(CONFIG('config_base)).mkString)
      else
        cfg_base.list.map(f =>
          io.Source.fromFile(CONFIG('config_base) + "/" + f).mkString)

    for (source <- sources){
      val xml = scala.xml.XML.loadString(source)

      var resources = List[scala.xml.Node]()
      var prepared  = List[scala.xml.Node]()

      if (xml.head.label == "resource")
        resources = List(xml.head)

      else if (xml.head.label == "prepared_query")
        prepared = List(xml.head)

      else {
        resources = (xml \ "resource").toList
        prepared = (xml \ "prepared_query").toList
      }

      for (elem <- resources) {
        val resource = new ResourceManifest(elem)
        log_debug("Loaded resource: " + resource.name)
        manifest += ((resource.name, resource))
      }

      for (elem <- prepared) {
        val pquery = new PreparedQuery(elem)
        log_debug("Loaded prepared_query: " + pquery.name)
        prepared_queries += ((pquery.name, pquery))
      }

    }
  }

  def usage(head: Boolean = true) = {
    if (head)
      println("sqltapd " + VERSION + " (c) 2012 Paul Asmuth\n")

    println("usage: sqltapd [options]                                                   ")
    println("  -c, -config       <dir>     path to xml config files                     ")
    println("  --http            <port>    start http server on this port               ")
    println("  --mysql-host      <addr>    mysql server hostname                        ")
    println("  --mysql-port      <port>    mysql server port                            ")
    println("  --mysql-user      <user>    mysql server username                        ")
    println("  --mysql-password  <pass>    mysql server password                        ")
    println("  --mysql-database  <db>      mysql server database (USE ...;)             ")
    println("  --memcached       <addrs>   comma-seperated memcache servers (host:port) ")
    println("  --memcached-ttl   <secs>    ttl for memcache keys                        ")
    println("  -h, --help                  you're reading it...                         ")
    println("  -d, --debug                 debug mode                                   ")
  }


  def log(msg: String) = {
    val now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.FRANCE)
    println("[" + now.format(new Date()) + "] " + msg)
  }

  def error(msg: String, fatal: Boolean) = {
    log("[ERROR] " + msg)

    if (fatal)
      System.exit(1)
  }


  def log_debug(msg: String) =
    if (debug)
      log("[DEBUG] " + msg)


  def exception(ex: Throwable, fatal: Boolean) = {
    error(ex.toString, false)

    for (line <- ex.getStackTrace)
      log_debug(line.toString)

    if (fatal)
      System.exit(1)
  }

}

object NullOutputStream extends java.io.OutputStream {
  def write(any : Int): Unit = {}
}
