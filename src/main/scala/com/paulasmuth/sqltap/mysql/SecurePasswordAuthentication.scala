// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import java.security.{MessageDigest}

// MySQL Secure Password Authenticaion:
//
//   resp = SHA1(passwd) XOR SHA1(CONCAT(challenge, SHA1(SHA1(password))))
//
// example:
//
//   password  -> "asd123"
//   challenge -> 5e7a6c2f4037323553214a304f7d4a397b233936
//   response  -> ea23519b85612f1f64277653f67cffae33ce8d2d
//
object SecurePasswordAuthentication {

  val sha1_md = MessageDigest.getInstance("SHA")

  def auth(req: HandshakePacket, password: String) : Array[Byte] = auth(
    concat(req.authp_data1._1.getBytes, req.authp_data2.getBytes),
    password)

  def auth(challenge: Array[Byte], password: String) : Array[Byte] = xor(
    sha1(password.getBytes),
    sha1(concat(challenge, sha1(sha1(password.getBytes)))))

  private def sha1(in: Array[Byte]) : Array[Byte] = {
    sha1_md.update(in)
    sha1_md.digest
  }

  private def xor(left: Array[Byte], right: Array[Byte]) : Array[Byte] = {
    var res = new Array[Byte](left.size)

    if (left.size != right.size)
      throw new Exception("arrays must be the of same length")

    for (n <- (0 until left.size))
      res(n) = (left(n) ^ right(n)).toByte

    return res
  }

  private def concat(left: Array[Byte], right: Array[Byte]): Array[Byte] = {
    var res = new Array[Byte](left.size + right.size)

    System.arraycopy(left,  0, res, 0,           left.length)
    System.arraycopy(right, 0, res, left.length, right.length)

    return res
  }

}

