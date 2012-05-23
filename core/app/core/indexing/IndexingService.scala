package core.indexing

import com.mongodb.casbah.commons.TypeImports._
import core.search.SolrServer
import core.Constants._
import play.api.Logger
import org.apache.solr.common.SolrInputDocument
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.request.UpdateRequest

/**
 * Indexing API
 */
object IndexingService extends SolrServer {

  val COMMIT_WITHIN = 5000

  /**
   * Stages a SOLR InputDocument for indexing, and applies all generic delving mechanisms on top
   */
  def stageForIndexing(doc: SolrInputDocument) = {
    import scala.collection.JavaConversions._

    val hasDigitalObject: Boolean = !doc.entrySet().filter(entry => entry.getKey.startsWith(THUMBNAIL) && !entry.getValue.isEmpty).isEmpty
    if (doc.containsKey(HAS_DIGITAL_OBJECT)) doc.remove(HAS_DIGITAL_OBJECT)
    doc.addField(HAS_DIGITAL_OBJECT, hasDigitalObject)

    if (hasDigitalObject) doc.setDocumentBoost(1.4.toFloat)

    if (!doc.containsKey(VISIBILITY)) {
      doc addField(VISIBILITY, "10") // set to public by default
    }

    // standard facets
    doc.addField(RECORD_TYPE + "_facet", doc.getField(RECORD_TYPE).getFirstValue)
    doc.addField(HAS_DIGITAL_OBJECT + "_facet", hasDigitalObject)

    // see http://wiki.apache.org/solr/CommitWithin
    getStreamingUpdateServer.add(doc, COMMIT_WITHIN)
  }

  /**
   * Commits staged Things or MDRs to index
    */
  def commit() = {
    getStreamingUpdateServer.commit(false, false)
  }

  /**
   * Rolls back staged indexing requests
   */
  def rollback() {
    getStreamingUpdateServer.rollback()
  }

  /**
   * Deletes from the index by string ID
   */
  def deleteById(id: String) {
    getStreamingUpdateServer.deleteById(id, COMMIT_WITHIN)
  }

  /**
   * Deletes from the index by ObjectId
   */
  def deleteById(id: ObjectId) {
    deleteById(id.toString)
  }

  /**
   * Deletes from the index by query
   */
  def deleteByQuery(query: String) {
    SolrServer.deleteFromSolrByQuery(query)
  }

  /**
   * Deletes from the index by collection spec
   */
  def deleteBySpec(orgId: String, spec: String) {
    val deleteQuery = SPEC + ":" + spec + " " + ORG_ID + ":" + orgId
    Logger.info("Deleting dataset from Solr Index: %s".format(deleteQuery))
    val deleteResponse = getStreamingUpdateServer.deleteByQuery(deleteQuery, COMMIT_WITHIN)
    deleteResponse.getStatus
  }

  def deleteOrphansBySpec(orgId: String, spec: String, startIndexing: DateTime) {
    val fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val deleteQuery = SPEC + ":" + spec + " AND " + ORG_ID + ":" + orgId + " AND timestamp:[* TO " + fmt.print(startIndexing.minusSeconds(15)) + "]"
    val orphans = getSolrServer.query(new SolrQuery(deleteQuery)).getResults.getNumFound
    if (orphans > 0) {
      try {
        val deleteResponse = getStreamingUpdateServer.deleteByQuery(deleteQuery, COMMIT_WITHIN)
        deleteResponse.getStatus
        Logger.info("Deleting orphans %s from dataset from Solr Index: %s".format(orphans.toString, deleteQuery))
      }
      catch {
        case e: Exception => Logger.info("Unable to remove orphans for %s because of %s".format(spec, e.getMessage))
      }
    }
    else
      Logger.info("No orphans found for dataset in Solr Index: %s".format(deleteQuery))

  }

}

