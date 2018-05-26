package com.paulasmuth.sqltap

import java.nio.ByteBuffer

class JSONWriterSpec extends UnitSpec {

  describe("write_escaped") {
    it("0x20 -> ' '") {
      val str = write_escaped(Array(0x20.toByte))
      assert(str == " ")
    }

    it("0xA -> '\\n'") {
      val str = write_escaped(Array(0xA.toByte))
      assert(str == "\\n")
    }

    it("0x22 -> '\\\"'") {
      val str = write_escaped(Array(0x22.toByte))
      assert(str == "\\\"")
    }

    it("0x5C -> '\\\\'") {
      val str = write_escaped(Array(0x5C.toByte))
      assert(str == "\\\\")
    }

    it("0xB -> '\\u000b'") {
      val str = write_escaped(Array(0xB.toByte))
      assert(str == "\\u000b")
    }
  }

  def buffer_to_string(buf: ByteBuffer) : String = {
    val bytes = new Array[Byte](buf.position())
    buf.rewind()
    buf.get(bytes)
    new String(bytes, "UTF-8")
  }

  def write_escaped(in_bytes: Array[Byte]) : String = {
    val buf = ByteBuffer.allocate(10)
    val writer = new JSONWriter(new WrappedBuffer(buf))
    writer.write_escaped(new String(in_bytes, "UTF-8"))
    buffer_to_string(buf)
  }

}
