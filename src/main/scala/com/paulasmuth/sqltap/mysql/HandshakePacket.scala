// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class HandshakePacket() extends SQLServerIssuedPacket {

  var capabilities : Long = 0
  var server_ver   : (String, Int) = null
  var authp_len    : Int = 0
  var authp_name   : (String, Int) = null
  var authp_data1  : (String, Int) = null
  var authp_data2  : String = null
  var conn_id      : Int = 0
  var charset      : Int = 0
  var status_flags : Int = 0

  def load(data: Array[Byte]) : Unit = {
    var cur = 1

    // 0x0a == MySQL Protocol Version 10
    if (data(0) != 0x0a)
      throw new SQLProtocolError(
        "unsupported mysql protocol version: " + data(0).toInt)

    server_ver     = BinaryString.read_null(data, cur)
    cur            = server_ver._2

    conn_id        = BinaryInteger.read(data, cur, 4)
    cur           += 4

    authp_data1    = BinaryString.read_null(data, cur)
    cur            = authp_data1._2

    capabilities   = BinaryInteger.read(data, cur, 2).toLong
    cur           += 2

    // old mysql versions send a short handshake packet
    if (cur + 1 >= data.size)
      throw new SQLProtocolError("mysql server version too old")

    charset        = BinaryInteger.read(data, cur, 1)
    cur           += 1

    status_flags   = BinaryInteger.read(data, cur, 2)
    cur           += 2

    capabilities  += BinaryInteger.read(data, cur, 2).toLong << 16L
    cur           += 2

    authp_len     += BinaryInteger.read(data, cur, 1)
    cur           += 11

    // check for CLIENT_PROTOCOL_41 flag (0x00000200)
    if ((capabilities & 0x00000200) != 0x00000200)
      throw new SQLProtocolError(
        "mysql server does not support CLIENT_PROTOCOL_41")

    // check for CLIENT_SECURE_CONNECTION flag (0x00008000)
    if ((capabilities & 0x00008000) != 0x00008000)
      throw new SQLProtocolError(
        "mysql server does not support CLIENT_SECURE_CONNECTION")

    authp_len     = Math.max(12, authp_len - 8)
    authp_data2   = BinaryString.read(data, cur, authp_len)
    cur          += authp_len

    // check for CLIENT_PLUGIN_AUTH flag (0x00080000)
    if ((capabilities & 0x00080000L) != 0x00080000L)
      return

    authp_name    = BinaryString.read_null(data, cur)
  }

}
