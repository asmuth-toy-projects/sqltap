package com.paulasmuth.sqltap

object JSONHelper {

  val unprintableChars = (List.range(0,9) ++ List.range(13,28) ++ List(127)).map(c => c.toChar.toString) // control chars, key codes, and DEL
  val whitespaceChars = List.range(11,13).map(c => c.toChar.toString) // VT and FF
  val separatorChars = List.range(28,32).map(c => c.toChar.toString) // group, unit and other separators (GS, US etc.)

  val sub_table = List(
    (("\\\\", """\\\\""")),
    (("\"", """\\"""")),
    (("\n", "\\\\n")),
    (("\t", "\\\\t"))
  )
  
  def escape(str: String): String = {
    var out = sub_table.foldLeft(str) ((s, cur) => 
      s.replaceAll(cur._1, cur._2))
    out = replaceChars(out, unprintableChars, "")
    out = replaceChars(out, whitespaceChars, " ")
    replaceChars(out, separatorChars, " ")
  }

  def replaceChars(str:String, chars:List[String], rplc:String): String =
    chars.foldLeft(str) ((s, c) =>
      s.replaceAll(c, rplc))
}
