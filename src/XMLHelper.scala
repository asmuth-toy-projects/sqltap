package com.paulasmuth.sqltap

class XMLHelper(elem: xml.Node) {

  def attr(name: String, required: Boolean = false, default: String = null) = {

    val value = elem.attribute(name).getOrElse(null)

    if (value != null)
      value.text

    else if (required) throw new ParseException(
     "missing attribute: " + name + " => " + elem.text)

    else
      (if (default != null) default else null)

  }

  def to_xml : String =
    elem.toString

}

