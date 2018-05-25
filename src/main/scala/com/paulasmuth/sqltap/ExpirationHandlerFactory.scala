// This file is part of the "SQLTap" project
//   (c) 2014 Paul Asmuth, Google Inc. <asmuth@google.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object ExpirationHandlerFactory {

  private var handler : ExpirationHandler = null

  /**
   * Returns a new ExpirationHandler
   *
   * @return a new ExpirationHandler
   */
  def get() : ExpirationHandler = {
    if (handler == null)
      throw new ExecutionException(
        "expiration handler not configured")

    handler
  }

  def configure(name: String) : Unit = {
    name match {

      case "noop" =>
        handler = new NoopExpirationHandler()

      case "purge" => {
        handler = new PurgeExpirationHandler()
        ReplicationFeed.start()
      }

      case _ =>
        throw new ParseException("unknown expiration handler: " + name)

    }
  }

}
