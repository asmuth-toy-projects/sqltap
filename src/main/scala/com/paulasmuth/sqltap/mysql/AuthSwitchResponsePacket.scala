// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import java.nio.{ByteBuffer}

class AuthSwitchResponsePacket() extends SQLClientIssuedPacket {

  private var auth_resp : Array[Byte] = null

  def write(buf: ByteBuffer) : Unit = {
    buf.put(auth_resp)
    buf.put(0x00.toByte)
  }

  def length() : Int =
    auth_resp.size +1

  def set_auth_resp(data: Array[Byte]) =
    auth_resp = data

}
