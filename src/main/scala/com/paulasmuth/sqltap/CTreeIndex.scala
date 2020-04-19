// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap,ListBuffer}

object CTreeIndex {

  val ctrees = new HashMap[String, ListBuffer[CTree]]

  def register(ctree: CTree) = {
    find(ctree.resource_name) += ctree
  }

  def find(resource_name: String) : ListBuffer[CTree] = {
    if (!ctrees.contains(resource_name)) {
      ctrees(resource_name) = new ListBuffer[CTree]()
    }

    ctrees(resource_name)
  }

  def find(root: Instruction) : Option[(CTree, Int)] = {
    val candidates = find(root.relation.resource.name)
    var winner : CTree = null
    var winner_cost : Int = -1
    var top_score : Int = 2

    for (ctree <- candidates) {
      var (score, cost) = ctree.compare(root)
      score += ctree.base_score

      Logger.debug("CTree: evaluating candidate: '" + ctree.name +
                   "' (score: " + score + ", cost: " + cost + ") for: " +
                   root.resource_name)

      val matches = (cost == 0 && winner_cost > 0) ||
                    (score > top_score && winner_cost != 0) ||
                    (score == top_score && cost > winner_cost)

      if (matches) {
        winner = ctree
        winner_cost = cost
        top_score = score
      }
    }

    if (winner != null)
      Logger.debug("CTree: using ctree '" + winner.name + "'")

    if (winner == null)
      None
    else
      Some(((winner, winner_cost)))
  }

}
