package controllers.admin

import controllers.SolrServer

/**
 * This Controller is responsible
 *
 *
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @since 7/7/11 12:04 AM  
 */

object Datasets extends SolrServer {

  import play.mvc.results.Result
  import org.apache.solr.client.solrj.SolrServer
  import play.mvc.Http
  import java.io.{OutputStream, InputStream}
  import java.util.zip.ZipOutputStream
  import eu.delving.metadata.{Facts, RecordMapping, MetadataModel}
  import eu.delving.sip.{DataSetInfo, DataSetResponse, DataSetResponseCode, AccessKey}
  import play.mvc.results.RenderText
  import play.mvc.results.RenderXml
  import org.apache.log4j.Logger
  import cake.ComponentRegistry
  import models.DataSet
  import xml.Elem

  private val RECORD_STREAM_CHUNK: Int = 1000
  private val log: Logger = Logger.getLogger(getClass)

//  private val metaRepo: MetaRepo = ComponentRegistry.metaRepo
  private val solrServer: SolrServer = getSolrServer

  private val metadataModel: MetadataModel = ComponentRegistry.metadataModel
//
  private val accessKeyService: AccessKey = ComponentRegistry.accessKey

  def secureListAll: Result = {
    try {
      new RenderXml(renderDataSetList(dataSets = DataSet.findAll))
    }
    catch {
      case e: Exception => renderException(e)
    }
  }

  def renderDataSetList(responseCode: DataSetResponseCode = DataSetResponseCode.THANK_YOU,
                        dataSets: List[DataSet] = List[DataSet](),
                        errorMessage: String = "") : Elem = {
    <data-set>
      <data-set-list>
        {dataSets.map{ds => ds.toXml}}
      </data-set-list>
      {if (responseCode != DataSetResponseCode.THANK_YOU) {
          <errorMessage>{errorMessage}</errorMessage>
        }
      }
    </data-set>
  }

  def listAll(accessKey: String): Result = {
    try {
      import play.mvc.results.RenderXml
      checkAccessKey(accessKey)
      new RenderXml(renderDataSetList(dataSets = DataSet.findAll))
    }
    catch {
      case e: Exception => renderException(e)
    }
  }

//  @RequestMapping(value = Array("/administrator/dataset/{dataSetSpec}/{command}"))
  def secureIndexingControl(dataSetSpec: String, command: String): Result = {
//    indexingControlInternal(dataSetSpec, command)
    new RenderText("something")
  }

//  @RequestMapping(value = Array("/dataset/{dataSetSpec}/{command}")) 
  def indexingControl(dataSetSpec: String, command: String, accessKey: String): Result = {
//    try {
//      checkAccessKey(accessKey)
//      return indexingControlInternal(dataSetSpec, command)
//    }
//    catch {
//      case e: Exception => {
//        return view(e)
//      }
//    }
    new RenderText("something")
  }

  private def checkAccessKey(accessKey: String) {
    import cake.metaRepo.AccessKeyException
    if (accessKey.isEmpty) {
      log.warn("Service Access Key missing")
      throw new AccessKeyException("Access Key missing")
    }
    else if (!accessKeyService.checkKey(accessKey)) {
      log.warn(String.format("Service Access Key %s invalid!", accessKey))
      throw new AccessKeyException(String.format("Access Key %s not accepted", accessKey))
    }
  }

//  @RequestMapping(value = Array("/dataset/submit/{dataSetSpec}/{fileType}/{fileName}"), method = Array(RequestMethod.POST))
  def acceptFile(dataSetSpec: String, fileType: String, fileName: String, inputStream: InputStream, accessKey: String): Result = {
    import play.mvc.results.RenderXml
    try {
      import eu.delving.metadata.Hasher
      import java.util.zip.GZIPInputStream
      checkAccessKey(accessKey)
      log.info(String.format("accept type %s for %s: %s", fileType, dataSetSpec, fileName))
      var hash: String = Hasher.extractHashFromFileName(fileName)
      if (hash == null) {
        throw new RuntimeException("No hash available for file name " + fileName)
      }
      val responseCode = fileType match {
        case "text/plain" => receiveFacts(Facts.read(inputStream), dataSetSpec, hash)
        case "application/x-gzip" => receiveSource(new GZIPInputStream(inputStream), dataSetSpec, hash)
        case "text/xml" => receiveMapping(RecordMapping.read(inputStream, metadataModel), dataSetSpec, hash)
        case _ => DataSetResponseCode.SYSTEM_ERROR
      }
      new RenderXml(renderDataSetList(responseCode = responseCode))
    }
    catch {
      case e: Exception => renderException(e)
    }

  }

//  @RequestMapping(value = Array("/dataset/fetch/{dataSetSpec}-sip.zip"), method = Array(RequestMethod.GET))
  def fetchSIP(dataSetSpec: String, accessKey: String, response: Http.Response): Unit = {
//    try {
//      import org.apache.commons.httpclient.HttpStatus
//      checkAccessKey(accessKey)
//      log.info(String.format("requested %s-sip.zip", dataSetSpec))
//      response.setContentType("application/zip")
//      writeSipZip(dataSetSpec, response.getOutputStream, accessKey)
//      response.setStatus(HttpStatus.OK.value)
//      log.info(String.format("returned %s-sip.zip", dataSetSpec))
//    }
//    catch {
//      case e: Exception => {
//        import org.apache.commons.httpclient.HttpStatus
//        response.setStatus(HttpStatus.BAD_REQUEST.value)
//        log.warn("Problem building sip.zip", e)
//      }
//    }
  }

  private def writeSipZip(dataSetSpec: String, outputStream: OutputStream, accessKey: String): Unit = {
    //    import java.util.zip.{ZipEntry, ZipOutputStream}
    //    var dataSet: MetaRepo.DataSet = metaRepo.getDataSet(dataSetSpec)
    //    if (dataSet == null) {
    //      import java.io.IOException
    //      throw new IOException("Data Set not found")
    //    }
    //    var zos: ZipOutputStream = new ZipOutputStream(outputStream)
    //    zos.putNextEntry(new ZipEntry(FileStore.FACTS_FILE_NAME))
    //    var facts: Facts = Facts.fromBytes(dataSet.getDetails.getFacts)
    //    facts.setDownloadedSource(true)
    //    zos.write(Facts.toBytes(facts))
    //    zos.closeEntry
    //    zos.putNextEntry(new ZipEntry(FileStore.SOURCE_FILE_NAME))
    //    var sourceHash: String = writeSourceStream(dataSet, zos, accessKey)
    //    zos.closeEntry
    //    for (mapping <- dataSet.mappings.values) {
    //      import eu.delving.metadata.RecordMapping
    //      var recordMapping: RecordMapping = mapping.getRecordMapping
    //      zos.putNextEntry(new ZipEntry(String.format(FileStore.MAPPING_FILE_PATTERN, recordMapping.getPrefix)))
    //      RecordMapping.write(recordMapping, zos)
    //      zos.closeEntry
    //    }
    //    zos.finish
    //    zos.close
    //    dataSet.setSourceHash(sourceHash, true)
    //    dataSet.save
  }

//  private def writeSourceStream(dataSet: MetaRepo.DataSet, zos: ZipOutputStream, accessKey: String): String = {
//    import eu.delving.metadata.SourceStream
//    import org.bson.types.ObjectId
//    var sourceStream: SourceStream = new SourceStream(zos)
//    sourceStream.startZipStream(dataSet.getNamespaces.toMap)
//    var afterId: ObjectId = null
//    while (true) {
//      import play.modules.legacyServices.eu.delving.core.MetaRepo.DataSet.RecordFetch
////      var fetch: MetaRepo.DataSet#RecordFetch = dataSet.getRecords(dataSet.getDetails.getMetadataFormat.getPrefix, RECORD_STREAM_CHUNK, null, afterId, null, accessKey)
////      if (fetch == null) {
////        break //todo: break is not supported
////      }
//      afterId = fetch.getAfterId
//      for (record <- fetch.getRecords) {
//        sourceStream.addRecord(record.getXmlString)
//      }
//    }
//    sourceStream.endZipStream
//  }

  private def receiveMapping(recordMapping: RecordMapping, dataSetSpec: String, hash: String): DataSetResponseCode = {
//    var dataSet: MetaRepo.DataSet = metaRepo.getDataSet(dataSetSpec)
//    if (dataSet == null) {
//      return datasetresponsecode.DATA_SET_NOT_FOUND
//    }
//    if (hasHash(hash, dataSet)) {
//      return DataSetResponseCode.GOT_IT_ALREADY
//    }
//    dataSet.setMapping(recordMapping, true)
//    dataSet.setMappingHash(recordMapping.getPrefix, hash)
//    dataSet.save
    DataSetResponseCode.THANK_YOU
  }

  private def receiveSource(inputStream: InputStream, dataSetSpec: String, hash: String): DataSetResponseCode = {
//    var dataSet: MetaRepo.DataSet = metaRepo.getDataSet(dataSetSpec)
//    if (dataSet == null) {
//      return DataSetResponseCode.DATA_SET_NOT_FOUND
//    }
//    if (hasHash(hash, dataSet)) {
//      return DataSetResponseCode.GOT_IT_ALREADY
//    }
//    dataSet.parseRecords(inputStream)
//    dataSet.setSourceHash(hash, false)
//    val details: MetaRepo.Details = dataSet.getDetails
//    details.setTotalRecordCount(dataSet.getRecordCount)
//    details.setDeletedRecordCount(details.getTotalRecordCount - details.getUploadedRecordCount)
//    dataSet.save
    DataSetResponseCode.THANK_YOU
  }

  private def receiveFacts(facts: Facts, dataSetSpec: String, hash: String): DataSetResponseCode = {
//    import eu.delving.metadata.{MetadataException, MetadataNamespace}
//    var dataSet: MetaRepo.DataSet = metaRepo.getDataSet(dataSetSpec)
//    if (dataSet == null) {
//      dataSet = metaRepo.createDataSet(dataSetSpec)
//    }
//    if (hasHash(hash, dataSet)) {
//      return DataSetResponseCode.GOT_IT_ALREADY
//    }
//    var details: MetaRepo.Details = dataSet.createDetails
//    details.setName(facts.get("name"))
//    details.setUploadedRecordCount(Integer.parseInt(facts.getRecordCount))
//    details.setTotalRecordCount(-1)
//    details.setDeletedRecordCount(-1)
//    var prefix: String = facts.get("namespacePrefix")
//    for (metadataNamespace <- MetadataNamespace.values) {
//      if (metadataNamespace.getPrefix == prefix) {
//        details.getMetadataFormat.setPrefix(prefix)
//        details.getMetadataFormat.setNamespace(metadataNamespace.getUri)
//        details.getMetadataFormat.setSchema(metadataNamespace.getSchema)
//        details.getMetadataFormat.setAccessKeyRequired(true)
//        break //todo: break is not supported
//      }
//    }
//    dataSet.setFactsHash(hash)
//    try {
//      details.setFacts(Facts.toBytes(facts))
//    }
//    catch {
//      case e: MetadataException => {
//        return DataSetResponseCode.SYSTEM_ERROR
//      }
//    }
//    dataSet.save
    DataSetResponseCode.THANK_YOU
  }



  private def indexingControlInternal(dataSetSpec: String, commandString: String): Result = {
//    try {
//      import eu.delving.sip.{DataSetState, DataSetCommand}
//      var dataSet: MetaRepo.DataSet = metaRepo.getDataSet(dataSetSpec)
//      if (dataSet == null) {
//        throw new DataSetNotFoundException(String.format("String %s does not exist", dataSetSpec))
//      }
//      var command: DataSetCommand = DataSetCommand.valueOf(commandString)
//      var state: DataSetState = dataSet.getState(false)
//      command match {
//        case DISABLE =>
//          state match {
//            case QUEUED =>
//            case INDEXING =>
//            case ERROR =>
//            case ENABLED =>
//              dataSet.setState(DataSetState.DISABLED)
//              dataSet.setRecordsIndexed(0)
//              dataSet.save
//              deleteFromSolr(dataSet)
//              view(dataSet)
//            case _ =>
//              view(DataSetResponseCode.STATE_CHANGE_FAILURE)
//          }
//        case INDEX =>
//          state match {
//            case DISABLED =>
//            case UPLOADED =>
//              dataSet.setState(DataSetState.QUEUED)
//              dataSet.save
//              view(dataSet)
//            case _ =>
//              view(DataSetResponseCode.STATE_CHANGE_FAILURE)
//          }
//        case REINDEX =>
//          state match {
//            case ENABLED =>
//              dataSet.setRecordsIndexed(0)
//              dataSet.setState(DataSetState.QUEUED)
//              dataSet.save
//              view(dataSet)
//            case _ =>
//              view(DataSetResponseCode.STATE_CHANGE_FAILURE)
//          }
//        case DELETE =>
//          state match {
//            case INCOMPLETE =>
//            case DISABLED =>
//            case ERROR =>
//            case UPLOADED =>
//              dataSet.delete
//              dataSet.setState(DataSetState.INCOMPLETE)
//              view(dataSet)
//            case _ =>
//              view(DataSetResponseCode.STATE_CHANGE_FAILURE)
//          }
//        case _ =>
//          throw new RuntimeException
//      }
//    }
//    catch {
//      case e: Exception => {
//        view(e)
//      }
//    }
    new RenderText("something")
  }

//  private def deleteFromSolr(dataSet: MetaRepo.DataSet): Unit = {
//    import org.apache.solr.client.solrj.response.UpdateResponse
//    import org.apache.solr.common.util.NamedList
//    val deleteResponse: UpdateResponse = solrServer.deleteByQuery("europeana_collectionName:" + dataSet.getSpec)
//    val responseHeader: NamedList[_] = deleteResponse.getResponseHeader
//    solrServer.commit
//  }

  private def view(responseCode: DataSetResponseCode): Result = {
    //    import eu.delving.sip.DataSetResponse
//    view(new DataSetResponse(responseCode))
    new RenderText("something")
  }

  private def renderException(exception: Exception): Result = {
    import cake.metaRepo.{DataSetNotFoundException, AccessKeyException}
    import play.mvc.results.RenderXml
    log.warn("Problem in controller", exception)
    val errorcode = exception match {
      case x if x.isInstanceOf[AccessKeyException] => DataSetResponseCode.ACCESS_KEY_FAILURE
      case x if x.isInstanceOf[DataSetNotFoundException] => DataSetResponseCode.DATA_SET_NOT_FOUND
      case _ => DataSetResponseCode.SYSTEM_ERROR
    }
    new RenderXml(renderDataSetList(responseCode = errorcode, errorMessage = exception.getMessage))
  }

//  private def view(dataSet: MetaRepo.DataSet): Result = {
//    import eu.delving.sip.DataSetResponse
//    if (dataSet == null) {
//      throw new DataSetNotFoundException("Data Set was null")
//    }
//    var response: DataSetResponse = new DataSetResponse(DataSetResponseCode.THANK_YOU)
//    response.addDataSetInfo(getInfo(dataSet))
//    new Result("dataSetXmlView", BindingResult.MODEL_KEY_PREFIX + "response", response)
//    new RenderText("something")
//  }

//  private def view(dataSetList: Collection[_ <: MetaRepo.DataSet]): Result = {
    //    var response: DataSetResponse = new DataSetResponse(DataSetResponseCode.THANK_YOU)
//    for (dataSet <- dataSetList) {
//      response.addDataSetInfo(getInfo(dataSet))
//    }
//    view(response)
//    new RenderText("something")
//  }

  private def render(response: DataSetResponse): Result = {
    //    new Result("dataSetXmlView", BindingResult.MODEL_KEY_PREFIX + "response", response)
    new RenderText("something")
  }

//  private def hasHash(hash: String, dataSet: MetaRepo.DataSet): Boolean = dataSet.getHashes.contains(hash)
//
//  private def getInfo(dataSet: MetaRepo.DataSet): DataSetInfo = {
//    val info: DataSetInfo = new DataSetInfo
//    info.spec = dataSet.getSpec
//    info.name = dataSet.getDetails.getName
//    info.state = dataSet.getState(false).toString
//    info.recordCount = dataSet.getRecordCount
//    info.errorMessage = dataSet.getErrorMessage
//    info.recordsIndexed = dataSet.getRecordsIndexed
//    info.hashes = dataSet.getHashes
//    info
//  }
}