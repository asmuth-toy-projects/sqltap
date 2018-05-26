// This file is part of the "SQLTap" project
//   (c) 2014 Paul Asmuth, Google Inc. <asmuth@google.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.ByteBuffer
import java.net.URLDecoder
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

class HTTPParseError extends Exception

class HTTPParser {

  private val HTTP_STATE_METHOD  = 1
  private val HTTP_STATE_URI     = 2
  private val HTTP_STATE_VERSION = 3
  private val HTTP_STATE_HKEY    = 4
  private val HTTP_STATE_HVAL    = 5

  var http_method  : String = null
  var http_uri     : String = null
  var http_version : String = null
  var http_headers = new HashMap[String, String]

  private var pos   = 0
  private var token = 0
  private var state = HTTP_STATE_METHOD
  private val buf   = new Array[Byte](4096)
  private var hkey  : String = null

  def read(src: ByteBuffer) : Boolean = {
    val max = src.position

    while (pos < max) {
      src.get(pos) match {

        case ' ' => state match {

          case HTTP_STATE_METHOD => {
            http_method = next_token(src)
            state = HTTP_STATE_URI
          }

          case HTTP_STATE_URI => {
            http_uri = next_token(src)
            state = HTTP_STATE_VERSION
          }

          case _ =>
            ()

        }

        case '\n' => state match {

          case HTTP_STATE_VERSION => {
            next_token(src) match {
              case "HTTP/1.0" => http_version = "1.0"
              case "HTTP/1.1" => http_version = "1.1"
              case _ => throw new HTTPParseError()
            }
            state = HTTP_STATE_HKEY
          }

          case HTTP_STATE_HVAL => {
            hkey = next_token(src)
            state = HTTP_STATE_HKEY
          }

          case HTTP_STATE_HKEY =>
            return true

          case _ =>
            ()

        }

        case ':' => state match {

          case HTTP_STATE_HKEY => {
            http_headers += ((hkey, next_token(src)))
            state = HTTP_STATE_HVAL
          }

          case _ =>
            ()

        }

        case _ =>
          ()

      }

      pos += 1
    }

    false
  }

  private def next_token(src: ByteBuffer) : String = {
    var start = 0
    var end   = pos - token

    System.arraycopy(src.array, token, buf, 0, pos - token)
    token = pos + 1

    while (buf(start) == ' ')
      start += 1

    while (buf(end - 1) == '\r' || buf(end - 1) == '\n')
      end -= 1

    new String(buf, start, end - start, "UTF-8")
  }

  def reset() : Unit =  {
    http_headers.clear
    pos          = 0
    token        = 0
    state        = HTTP_STATE_METHOD
    http_method  = null
    http_uri     = null
    http_version = null
  }

  def uri_parts() : List[String] = {
    val uri = URLDecoder.decode(http_uri)
    var pos = uri.length
    var cur = pos - 1
    var ret = new ListBuffer[String]()

    while (cur >= 0) {
      val c = uri.charAt(cur)

      if (c == '/' || c == '?' || c == '&') {
        if (cur + 1 != pos)
          uri.substring(cur + 1, pos) +=: ret

        pos = cur
      }

      cur -= 1
    }

    return ret.toList
  }

}
