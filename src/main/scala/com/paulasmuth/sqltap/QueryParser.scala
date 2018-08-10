// This file is part of the "SQLTap" project
//   (c) 2014 Paul Asmuth, Google Inc. <asmuth@google.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.ListBuffer

object QueryParser {

  private val PARSER_STATE_NEXT   = 1
  private val PARSER_STATE_CMD    = 2
  private val PARSER_STATE_ARG    = 3
  private val PARSER_STATE_ARGSTR = 4
  private val PARSER_STATE_BODY   = 5

  def parse(stack: InstructionStack, qry: String) : Unit = {

    if (Config.get('log_queries).equals("true")) {
      Logger.info("[QueryParser] parse: " + qry)
    }

    var args  = new ListBuffer[String]()
    var state = PARSER_STATE_NEXT

    if (qry == null)
      throw new ParseException("no query string")

    var cur = 0
    var len = qry.length
    var pos = 0

    while (cur < len) {
      qry.charAt(cur) match {

        case ' ' => state match {

          case PARSER_STATE_ARGSTR => ()

          case _ => pos = cur + 1

        }

        case '.' => state match {

          case PARSER_STATE_NEXT => {
            args += qry.substring(pos, cur)
            state = PARSER_STATE_CMD
            pos = cur + 1
          }

          case PARSER_STATE_ARGSTR => ()

        }

        case '(' => state match {

          case PARSER_STATE_CMD => {
            args += qry.substring(pos, cur)
            state = PARSER_STATE_ARG
            pos = cur + 1
          }

          case PARSER_STATE_ARGSTR => ()

        }

        case ',' => state match {

          case PARSER_STATE_ARG => {
            args += qry.substring(pos, cur)
            state = PARSER_STATE_ARG
            pos = cur + 1
          }

          case PARSER_STATE_NEXT => {
            if (cur != pos)
              stack.push_field(qry.substring(pos, cur))

            state = PARSER_STATE_NEXT
            pos = cur + 1
          }

          case PARSER_STATE_ARGSTR => ()

        }

        case ')' => state match {

          case PARSER_STATE_ARG => {
            args += qry.substring(pos, cur)
            state = PARSER_STATE_BODY
            pos = cur + 1
          }

          case PARSER_STATE_ARGSTR => ()

        }

        case '"' => state match {

          case PARSER_STATE_ARG => {
            state = PARSER_STATE_ARGSTR
          }

          case PARSER_STATE_ARGSTR => {
            state = PARSER_STATE_ARG
          }

        }

        case '{' => state match {

          case PARSER_STATE_BODY => {
            state = PARSER_STATE_NEXT

            stack.push_down(
              InstructionFactory.make(clean_args(args)))

            args.clear
            pos = cur + 1
          }

          case PARSER_STATE_CMD => {
            state = PARSER_STATE_NEXT
            args += qry.substring(pos, cur)

            stack.push_down(
              InstructionFactory.make(clean_args(args)))

            args.clear
            pos = cur + 1
          }

          case PARSER_STATE_ARGSTR => ()

        }

        case '}' => state match {

          case PARSER_STATE_NEXT => {
            state = PARSER_STATE_NEXT

            if (cur != pos)
              stack.push_field(qry.substring(pos, cur))

            stack.pop()
            pos = cur + 1
          }

        }

        case PARSER_STATE_ARGSTR => ()

        case _ => ()

      }

      cur += 1
    }
  }

  private def clean_args(args: ListBuffer[String]) : ListBuffer[String] = {
    for (ind <- (0 until args.length)) {

      if (args(ind).charAt(args(ind).length - 1) == '"')
        args(ind) = args(ind).substring(0, args(ind).length - 1)

      if (args(ind).charAt(0) == '"')
        args(ind) = args(ind).substring(1)

    }

    args
  }

}
