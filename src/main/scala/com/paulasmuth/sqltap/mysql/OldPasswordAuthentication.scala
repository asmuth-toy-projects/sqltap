// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import java.security.{MessageDigest}

// MySQL Old Password Authenticaion:
// example:
//
//   password  -> "fetch"
//   challenge -> 54365175797e2f6c
//   response  -> 5c51595f5957525d
//
object OldPasswordAuthentication {

  def auth(req: HandshakePacket, password: String) : Array[Byte] =
    auth(req.authp_data1._1, password).getBytes

  def auth(challenge: String, password: String) : String = {
    val chars       = new Array[Char](challenge.length)
    var b : Byte    = 0
    var d : Double  = 0
    val max : Long  = 0x3fffffffL

    var pw  = hash(challenge)
    var msg = hash(password)

    var seed1 : Long = (pw(0) ^ msg(0)) % max
    var seed2 : Long = (pw(1) ^ msg(1)) % max

    for (n <- (0 until challenge.length)) {
      seed1    = ((seed1 * 3) + seed2) % max
      seed2    = (seed1 + seed2 + 33) % max
      d        = seed1.toDouble / max.toDouble
      b        = java.lang.Math.floor((d * 31) + 64).toByte
      chars(n) = b.toChar
    }

    seed1 = ((seed1 * 3) + seed2) % max
    seed2 = (seed1 + seed2 + 33) % max

    d = seed1.toDouble / max.toDouble
    b = java.lang.Math.floor(d * 31).toByte

    for (n <- (0 until challenge.length))
      chars(n)  = (chars(n) ^ b.toChar).toChar

    return new String(chars)
  }

  private def hash(data: String) : Array[Long] = {
    var result     = new Array[Long](2)
    var nr  : Long = 1345345333L
    var add : Long = 7
    var nr2 : Long = 0x12345671L
    var tmp : Long = 0

    for (n <- (0 until data.size)) {
      if ((data.charAt(n) != ' ') && (data.charAt(n) != '\t')) {
        tmp  = (0xff & data.charAt(n))
        nr  ^= ((((nr & 63) + add) * tmp) + (nr << 8))
        nr2 += ((nr2 << 8) ^ nr)
        add += tmp
      }
    }

    result(0) = nr  & 0x7fffffffL
    result(1) = nr2 & 0x7fffffffL

    result
  }

}
