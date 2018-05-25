// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

trait ResourceRelation {
  val name : String
  val output_name : String
  def resource : ResourceManifest

  val join_foreign : Boolean
  val join_field : String
  val join_field_local : String
  val join_field_remote : String
  val join_cond : String
}


class DummyResourceRelation(_res: ResourceManifest) extends ResourceRelation {
  val name = _res.name
  val output_name = name

  val join_field : String = null
  val join_cond : String = null
  val join_field_local : String = null
  val join_field_remote : String = null
  val join_foreign : Boolean = false

  def resource : ResourceManifest =
    _res
}

class RealResourceRelation(doc: xml.Node) extends ResourceRelation {
  val elem = new XMLHelper(doc)

  val resource_name : String =
    elem.attr("resource", true)

  val name : String = 
    elem.attr("name", true)

  val output_name : String =
    elem.attr("output_name", false, name)

  val join_field : String =
    elem.attr("join_field", true)

  val join_field_local : String =
    elem.attr("join_field_local", false)

  val join_field_remote : String =
    elem.attr("join_field_remote", false)

  val join_cond : String =
    elem.attr("join_cond", false)

  val join_foreign : Boolean = 
    (elem.attr("join_foreign", false) == "true")

  def resource : ResourceManifest =
    Manifest.resource(resource_name)
}

