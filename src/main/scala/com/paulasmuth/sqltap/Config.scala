// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap}

object Config {

  var debug = false

  val defaults = HashMap[Symbol, String](
    'http_port                -> "8080",
    'http_request_timeout     -> "2500",
    'http_idle_timeout        -> "500",
    'http_keepalive           -> "true",
    'sql_timeout              -> "1000",
    'sql_queue_max_len        -> "250",
    'sql_max_connections      -> "3",
    'memcache_mode            -> "copy",
    'memcache_queue_max_len   -> "4096",
    'memcache_max_connections -> "10",
    'memcache_host            -> "127.0.0.1",
    'memcache_port            -> "11211",
    'threads                  -> "4",
    'log_queries              -> "false",
    'expiration_handler       -> "purge",
    'cache_backend            -> "memcache"
  )

  private val config = HashMap[Symbol,String]()

  def set(key: Symbol, value: String) : Unit = {
    config(key) = value
  }

  def get(key: Symbol) : String = {
    config.getOrElse(key,
      defaults.getOrElse(key, null))
  }

  def get() : Map[Symbol,String] = {
    (defaults ++ config).toMap
  }

  def has_key(key: Symbol) : Boolean = {
    config.contains(key)
  }

}
