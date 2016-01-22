// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

/**
 * When a "record" is expired, an expiration job is created for every CTree
 * descendant of the record's resource. Each expiration job is exeucted at
 * least once with just the record id, but might be executed a second time with
 * a full record if it depends on keys other than the primary record id.
 */
class ExpirationJob(worker: Worker, ctree: CTree) extends ReadyCallback[Record]  {

  /**
   * Holds all possible (unevaluated) keys for this ctree as (field, cond) tuples.
   * These keys need to be evaluated with the record data before they can be used
   * to purge/update the cache
   */
  private var keys = RelationTrace.lookup(ctree.resource_name)

  /**
   * Retrieve an expiration handler (currently always purge)
   */
  private val handler = ExpirationHandlerFactory.get()

  /**
   * Executes this expiration job with only the primary record id proveded. As
   * some CTree Cache keys might depend on fields other than the primary id,
   * this does not purge all the possible keys in all cases.
   *
   * @param record the primary record id
   */
  def execute(record_id: Int) : Unit = {
    val primary_id = ctree.resource.id_field

    Logger.debug(
      "[EXPIRE] resource '" + ctree.resource_name + "' with id #" +
      record_id.toString + " expired (1/2)")

    for (tuple <- keys) {
      if (tuple._1 == primary_id) {
        val key = ctree.key(tuple._1, record_id.toString, tuple._2)
        handler.execute(worker, key)
        keys = keys.filter(_ != tuple)
      }
    }
  }

  /**
   * Executes this expiration job with the full previous record provided. As
   * some CTree Cache keys might depend on fields other than the primary id,
   * the full record information might be neccessarry to purge all the keys.
   *
   * @param record the full record
   */
  def execute(record: Record) : Unit = {
    Logger.debug(
      "[EXPIRE] resource '" + record.resource.name + "' with id #" +
      record.id.toString + " expired (2/2)")

    for (tuple <- keys) {
      val key = ctree.key(tuple._1, record.get(tuple._1), tuple._2)
      handler.execute(worker, key)
    }
  }

  /**
   * Indicates wether all possible keys in this ExpirationJob have been
   * expired or if there are still pending keys
   */
  def pending() : Boolean = {
    keys.length > 0
  }

  /**
   * The ready() method is called by the RecordLookupJob and starts execution
   * of this ExpirationJob.
   *
   * @param record the record for which this job should be executed
   */
  def ready(record: Record) : Unit = {
    execute(record)
  }

  /**
   * The error() method is called if an error occurs in the RecordLookupJob
   * We ignore errors here...
   *
   * @param record the record (usually null in case of an error)
   * @param err    the exception that occurred
   */
  def error(record: Record, err: Throwable) : Unit = {
    ()
  }

}
