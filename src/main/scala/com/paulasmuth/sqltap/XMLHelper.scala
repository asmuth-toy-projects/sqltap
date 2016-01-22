// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class XMLHelper(elem: xml.Node) {

  def attr(name: String, required: Boolean = false, default: String = null) = {

    val value = elem.attribute(name).getOrElse(null)

    if (value != null)
      value.toString

    else if (required) throw new ParseException(
     "missing attribute: " + name + " => " + elem.toString)

    else
      (if (default != null) default else null)

  }

  def to_xml : String =
    elem.toString

}

