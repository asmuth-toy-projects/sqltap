// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

object CTreeCache {

  def store(worker: Worker, ctree: CTree, key: String, ins: CTreeInstruction) : Unit = {
    val buf       = new ElasticBuffer(65535 * 8)
    val ctree_buf = new CTreeBuffer(buf)

    CTreeMarshal.serialize(ctree_buf, ctree.stack.head, ins)

    val request = new CacheStoreRequest(key, buf, ctree.expire)
    request.worker = worker

    worker.cache.enqueue(request)
    worker.cache.flush()
  }

  def retrieve(worker: Worker, ctree: CTree, key: String, ins: CTreeInstruction) : Unit = {
    val request = new CacheGetRequest(key)
    request.instruction = ins
    request.worker = worker

    worker.cache.enqueue(request)
  }

  /**
   * Flushes the complete CTreeCache
   */
  def flush(worker: Worker) : Unit = {
    worker.cache.flush()
  }

  /**
   * Expires an entry from the CTreeCache
   *
   * @param worker the current worker thread
   * @param record_id the primary id of the resource/record to be expired
   * @param resource_name the name of the resource to be expired
   */
  def expire(worker: Worker, resource_name: String, record_id: Int) : Unit = {
    if (!Manifest.has_resource(resource_name))
      throw new ParseException("unknown resource: " + resource_name)

    val ctrees   = CTreeIndex.find(resource_name)
    val resource = Manifest.resource(resource_name)
    val lookup   = new RecordLookupJob(worker, resource)

    for (ctree <- ctrees) {
      val expire = new ExpirationJob(worker, ctree)
      expire.execute(record_id)

      if (expire.pending) {
        lookup.attach(expire)
      }
    }

    lookup.execute(record_id)
  }

  def expire(worker: Worker, record: Record) : Unit = {
    val ctrees = CTreeIndex.find(record.resource.name)

    for (ctree <- ctrees) {
      val job = new ExpirationJob(worker, ctree)
      job.execute(record)
    }
  }

}
