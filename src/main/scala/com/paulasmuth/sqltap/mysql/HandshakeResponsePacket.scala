// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import java.nio.{ByteBuffer}

class HandshakeResponsePacket(req: HandshakePacket) extends SQLClientIssuedPacket {

  private var username  : Array[Byte] = null
  private var auth_resp : Array[Byte] = null
  private var database  : Array[Byte] = null

  def write(buf: ByteBuffer) : Unit = {
    // cap flags: CLIENT_PROTCOL_41 | CLIENT_SECURE_CONNECTION
    var capabilities = 0x00000200 | 0x00008000

    // cap flag: CLIENT_CONNECT_WITH_DB (0x00000008)
    if (database != null)
      capabilities |= 0x00000008

    buf.putInt(capabilities)

    // max packet size: 16mb
    buf.putInt(0x01000000)

    // character set: utf8
    buf.put(0x21.toByte)

    // 23 bytes \0 (reserved)
    buf.put((new Array[Byte](23)))

    // username + \0 byte
    buf.put(username)
    buf.put(0x00.toByte)

    // auth_resp
    if (auth_resp == null) {
      buf.put(0x00.toByte)
    } else {
      buf.put(auth_resp.size.toByte)
      buf.put(auth_resp)
    }

    // database + \0 byte
    if (database != null) {
      buf.put(database)
      buf.put(0x00.toByte)
    }

    // auth plugin name
    if (req.authp_name != null) {
      buf.put(req.authp_name._1.getBytes)
      buf.put(0x00.toByte)
    }
  }

  def length() : Int = {
    var len = 0

    // static fields (caps, maxsize, charset, reserved)
    len += 4 + 4 + 24

    // username field + \0 byte
    len += username.size + 1

    // auth resp len + auth resp
    if (auth_resp == null)
      len += 1
    else
      len += auth_resp.size + 1

    // database + \0 byte
    if (database != null)
      len += database.size + 1

    // auth plugin name + \0 byte
    if (req.authp_name != null)
      len += req.authp_name._1.getBytes.size + 1

    return len
  }

  def set_username(username_str: String) =
    username = username_str.getBytes("UTF-8")

  def set_auth_resp(data: Array[Byte]) =
    auth_resp = data

  def set_database(database_str: String) =
    database = database_str.getBytes("UTF-8")

}
