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

object Logger {

  val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.FRANCE)

  def log(msg: String) = {
    println("[" + df.format(new Date()) + "] " + msg)
  }

  def fatal(msg: String) : Unit = {
    log("[FATAL] " + msg)
    System.exit(1)
  }

  def error(msg: String, fatal: Boolean) : Unit = {
    log("[ERROR] " + msg)

    if (fatal)
      System.exit(1)
  }

  def notice(msg: String) : Unit = {
    log("[NOTICE] " + msg)
  }

  def info(msg: String) : Unit = {
    log("[INFO] " + msg)
  }

  def debug(msg: String) : Unit = {
    if (Config.debug)
      log("[DEBUG] " + msg)
  }

  def exception(ex: Throwable, fatal: Boolean) = {
    Logger.error(ex.toString, false)

    for (line <- ex.getStackTrace)
      Logger.log(line.toString)

    if (fatal)
      System.exit(1)
  }

}
