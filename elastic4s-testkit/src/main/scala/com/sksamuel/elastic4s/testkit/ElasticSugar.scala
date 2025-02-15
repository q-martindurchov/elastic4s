package com.sksamuel.elastic4s.testkit

import com.sksamuel.elastic4s.requests.indexes.admin.RefreshIndexResponse
import com.sksamuel.elastic4s.{ElasticDsl, Indexes}
import org.scalatest.Suite
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/** Provides helper methods for things like refreshing an index, and blocking until an index has a certain count of
  * documents. These methods are very useful when writing tests to allow for blocking, iterative coding
  */
trait ElasticSugar extends ElasticDsl {
  this: Suite with ClientProvider =>

  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  // refresh all indexes
  def refreshAll(): RefreshIndexResponse = refresh(Indexes.All)

  // refreshes all specified indexes
  def refresh(indexes: Indexes): RefreshIndexResponse =
    client
      .execute {
        refreshIndex(indexes)
      }
      .await
      .result

  def blockUntilGreen(): Unit =
    blockUntil("Expected cluster to have green status") { () =>
      client
        .execute {
          clusterHealth()
        }
        .await
        .result
        .status
        .toUpperCase == "GREEN"
    }

  def blockUntil(explain: String)(predicate: () => Boolean): Unit = {

    var backoff = 0
    var done    = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try done = predicate()
      catch {
        case e: Throwable =>
          logger.warn("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting on: $explain")
  }

  def ensureIndexExists(index: String): Unit =
    if (!doesIndexExists(index))
      client.execute {
        createIndex(index)
      }.await

  def doesIndexExists(name: String): Boolean =
    client
      .execute {
        indexExists(name)
      }
      .await
      .result
      .isExists

  def deleteIndex(name: String): Unit =
    Try {
      client.execute {
        ElasticDsl.deleteIndex(name)
      }.await
    }

  def truncateIndex(index: String): Unit = {
    deleteIndex(index)
    ensureIndexExists(index)
    blockUntilEmpty(index)
  }

  def blockUntilDocumentExists(id: String, index: String): Unit =
    blockUntil(s"Expected to find document $id") { () =>
      client
        .execute {
          get(index, id)
        }
        .await
        .result
        .exists
    }

  def blockUntilCount(expected: Long, index: String): Unit =
    blockUntil(s"Expected count of $expected") { () =>
      val result = client
        .execute {
          search(index).matchAllQuery().size(0)
        }
        .await
        .result
      expected <= result.totalHits
    }

  def blockUntilCount(expected: Long, indexes: Indexes): Unit =
    blockUntil(s"Expected count of $expected") { () =>
      val result = client
        .execute {
          search(indexes).matchAllQuery().size(0)
        }
        .await
        .result
      expected <= result.totalHits
    }

  def blockUntilExactCount(expected: Long, index: String): Unit =
    blockUntil(s"Expected count of $expected") { () =>
      expected == client
        .execute {
          search(index).size(0)
        }
        .await
        .result
        .totalHits
    }

  def blockUntilEmpty(index: String): Unit =
    blockUntil(s"Expected empty index $index") { () =>
      client
        .execute {
          search(Indexes(index)).size(0)
        }
        .await
        .result
        .totalHits == 0
    }

  def blockUntilIndexExists(index: String): Unit =
    blockUntil(s"Expected exists index $index") { () =>
      doesIndexExists(index)
    }

  def blockUntilIndexNotExists(index: String): Unit =
    blockUntil(s"Expected not exists index $index") { () =>
      !doesIndexExists(index)
    }

  def blockUntilDocumentHasVersion(index: String, id: String, version: Long): Unit =
    blockUntil(s"Expected document $id to have version $version") { () =>
      client
        .execute {
          get(index, id)
        }
        .await
        .result
        .version == version
    }
}
