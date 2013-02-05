package com.paulasmuth.sqltap.test

import com.paulasmuth.sqltap.JSONHelper
import org.scalatest._

class JSONHelperSpec extends FlatSpec {

  behavior of "JSONHelper.escape"


  it should "escape backslashes" in {  
    val input = "\\"
    val output = JSONHelper.escape(input)
    assert(output === """\\""")
  }

  it should "escape double quotes" in {  
    val input = "\""
    val output = JSONHelper.escape(input)
    assert(output === "\\\"")
  }

  it should "remove carriage returns" in {  
    val input = "\r"
    val output = JSONHelper.escape(input)
    assert(output === "")
  }

  it should "escape newlines" in {  
    val input = "\n"
    val output = JSONHelper.escape(input)
    assert(output === """\n""")
  }

  it should "escape tabs" in {  
    val input = "\t"
    val output = JSONHelper.escape(input)
    assert(output === """\t""")
  }
  
  it should "strip unprintable characters" in {  
    val input = List(5,14,127).map(c => c.toChar).mkString
    val output = JSONHelper.escape(input)
    assert(output === "")
  }

  it should "convert VT and FF characters to whitespaces" in {  
    val input = List(11,12).map(c => c.toChar).mkString
    val output = JSONHelper.escape(input)
    assert(output === " "*2)
  }

  it should "convert separator characters to whitespaces" in {  
    val input = List.range(28,32).map(c => c.toChar).mkString
    val output = JSONHelper.escape(input)
    assert(output === " "*4)
  }

}