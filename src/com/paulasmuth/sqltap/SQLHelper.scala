// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object SQLHelper {

  def is_sql(str: String) : Boolean = {
    str.length > 6 && str.substring(0, 6).equals("SELECT")
  }

}
