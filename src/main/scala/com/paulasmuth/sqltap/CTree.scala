// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class CTree(doc: xml.Node) {
  val elem = new XMLHelper(doc)

  val name  : String = elem.attr("name", true)
  val query : String = elem.attr("query", true)
  val expire : Int = elem.attr("expire", false, "300").toInt

  val allow_conditions : Boolean =
    elem.attr("allow_conditions", false, "true").equals("true")

  val base_score : Int =
    elem.attr("base_score", false, "0").toInt

  val stack = new InstructionStack()
  QueryParser.parse(stack, query)

  if (!stack.head.fields.contains(stack.head.relation.resource.id_field))
    throw new ParseException(
      "a ctree's root instruction must fetch the id field")

  CTreeIndex.register(this)
  stack.head.inspect()

  /**
   * Returns the name of the resource this ctree is descendant from
   *
   * @return name of the resource
   */
  def resource_name() : String = {
    stack.head.relation.resource.name
  }

  /**
   * Returns the resource this ctree is descendant from
   *
   * @return the resource
   */
  def resource() : ResourceManifest = {
    stack.head.relation.resource
  }

  /**
   * Returns the relation this ctree is descendant from
   *
   * @return the relation
   */
  def relation() : ResourceRelation = {
    stack.head.relation
  }

  def key(join_key: String, record_id: String, join_cond: String) : String = {
    if (join_cond == null) {
      name + "/" + join_key + "/" + record_id
    } else {
      // FIXPAUL md5-hash conditions
      name + "/" + join_key + "/" + record_id + "/" + join_cond.replaceAll(" ", "")
    }
  }

  def compare(ins: Instruction) : (Int, Int) = {
    compare(ins, stack.head)
  }

  /**
   * compares two instructions and computes two "similarity" measures:
   *
   * - the "score" determines how much of the query tree can be substituted with
   * the ctree. a score of 0 means that no field in the query can be answered
   * from the ctree (larger is better)
   *
   * - the "cost" determines how much additional data the ctree contais that is not
   * required to answer the query which would be unecessarily fetched. a perfect
   * cost of 0 means no extraneous data is fetched (smaller is better)
   *
   * @return a tuple of (score, cost)
   */
  def compare(left: Instruction, right: Instruction) : (Int, Int) = {
    var score = 0
    var cost  = 0

    val lfields = left.fields.clone()

    for (rfield <- right.fields) {
      if (lfields.contains(rfield)) {
        score += 1
        lfields -= rfield
      } else {
        cost -= 1
      }
    }

    val inslist = left.next.clone()

    for (rins <- right.next) {
      var found = false
      var n     = inslist.length

      while (n > 0 && !found) {
        n -= 1

        val lins = inslist(n)

        if (lins.compare(rins)) {
          found = true

          val (cscore, ccost) = compare(lins, rins)
          score += 10 + cscore
          cost  += ccost

          inslist -= lins
        }
      }

      if (!found)
        cost -= 100
    }

    (score, cost)
  }

}
