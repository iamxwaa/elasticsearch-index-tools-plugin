package com.github.toxrink.indextools.tools

import java.io.File
import java.util
import java.util.{Calendar, Date}

import com.github.toxrink.indextools.core.config.AutoCreateIndex
import com.github.toxrink.indextools.core.constants.IndexToolsConstants
import com.github.toxrink.indextools.core.security.GrantRun
import org.apache.commons.io.FileUtils
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import x.utils.{JxUtils, TimeUtils}

/**
  * Created by xw on 2019/9/18.
  */
object AdminTools {
  private val logger = JxUtils.getLogger(AdminTools.getClass)

  private var recordIndexExist = false

  def checkAndCreateIndex(srcIndex: String, testAlias: String, client: Client): (Boolean, Boolean) = {
    val indexExist = new IndicesExistsRequest().indices(srcIndex)
    val isExist =
      client.admin().indices().exists(indexExist).actionGet().isExists
    if (!isExist) {
      val create = new CreateIndexRequest().index(srcIndex)
      client.admin().indices().create(create).actionGet()
      (true, false)
    } else {
      val aliasRequest =
        new GetAliasesRequest().indices(srcIndex).aliases(testAlias)
      val exist =
        client.admin().indices().aliasesExist(aliasRequest).actionGet().exists()
      (true, exist)
    }
  }

  def checkAndCreateIndex(srcIndex: String, client: Client): Boolean = {
    val indexExist = new IndicesExistsRequest().indices(srcIndex)
    val isExist =
      client.admin().indices().exists(indexExist).actionGet().isExists
    if (!isExist) {
      val create = new CreateIndexRequest().index(srcIndex)
      client.admin().indices().create(create).actionGet()
    }
    isExist
  }

  def addAlias(autoIndex: AutoCreateIndex, date: Date, createIndex: Boolean, onlyName: Boolean)(
      implicit client: Client): Unit = {
    val srcIndex = autoIndex.index(date, onlyName)
    val aliasIndex = autoIndex.alias(date)
    //别名为空,只检查创建索引
    if (aliasIndex.isEmpty) {
      checkAndCreateIndex(srcIndex, client)
      return
    }
    val indexCreateStatus =
      if (createIndex)
        checkAndCreateIndex(srcIndex, aliasIndex.head._1, client)
      else
        (true, false)
    if (!indexCreateStatus._2) {
      val request = client.admin().indices().prepareAliases()
      aliasIndex.foreach(as => {
        request.addAlias(
          srcIndex,
          as._1,
          QueryBuilders
            .rangeQuery(autoIndex.timeField)
            .from(as._2._1)
            .to(as._2._2)
        )
      })
      var status = true
      try {
        status = request.execute().actionGet().isAcknowledged
      } catch {
        case _: Throwable => status = false
      }
      writeRecord(srcIndex, status, aliasIndex, client)
    }
  }

  def addAlias(autoIndex: AutoCreateIndex, date: Date)(implicit client: Client): Unit = {
    addAlias(autoIndex, date, createIndex = true, onlyName = false)
  }

  def writeRecord(srcIndex: String, status: Boolean, aliasIndex: List[(String, (Date, Date))], client: Client): Unit = {
    //记录索引不存在则创建
    if (!recordIndexExist) {
      val indexExist = new IndicesExistsRequest().indices(IndexToolsConstants.ALIAS_RECORD_INDEX_NAME)
      val isExist =
        client.admin().indices().exists(indexExist).actionGet().isExists
      if (!isExist) {
        val settings = new util.HashMap[String, Object]()
        settings.put("number_of_shards", "1")
        settings.put("number_of_replicas", "1")
        val create =
          new CreateIndexRequest().index(IndexToolsConstants.ALIAS_RECORD_INDEX_NAME).settings(settings)
        client.admin().indices().create(create).actionGet()
      }
      recordIndexExist = true
    }

    //写入索引及别名创建记录
    val source = new util.HashMap[String, Object]()
    source.put("createStatus", status.toString)
    source.put(
      "aliasIndex",
      aliasIndex
        .map(as => {
          s"${as._1}#${TimeUtils.format(as._2._1, "yyyyMMddHHmmssSSS")}-${TimeUtils.format(as._2._2, "yyyyMMddHHmmssSSS")}"
        })
        .toArray
    )
    client
      .prepareIndex(IndexToolsConstants.ALIAS_RECORD_INDEX_NAME, IndexToolsConstants.TYPE)
      .setId(srcIndex)
      .setSource(source)
      .execute()
      .actionGet()
  }

  def preCreateIndex(autoIndexList: List[AutoCreateIndex], esClient: Client): Unit = {
    logger.info("Start preCreateIndex")
    autoIndexList.foreach(a => AdminTools.addAlias(a, TimeUtils.getNow)(esClient))
  }

  def createAlias(autoIndex: AutoCreateIndex, createIndex: Boolean, onlyName: Boolean, esClient: Client): Unit = {
    val start = Calendar.getInstance()
    start.setTime(autoIndex.history._1)
    start.add(autoIndex.increaseType._2, -1)
    val end = Calendar.getInstance()
    end.setTime(autoIndex.history._2)
    AdminTools.addAlias(autoIndex, start.getTime, createIndex, onlyName)(esClient)
    while (start.before(end)) {
      AdminTools.addAlias(autoIndex, start.getTime, createIndex, onlyName)(esClient)
      start.add(autoIndex.increaseType._2, 1)
    }
  }

  def createHistoryIndex(autoIndexList: List[AutoCreateIndex], esClient: Client): Unit = {
    logger.info("Start createHistoryIndex")
    autoIndexList.foreach(a => {
      if (null != a.history) {
        val start = Calendar.getInstance()
        start.setTime(a.history._1)
        start.add(a.increaseType._2, -1)
        val end = Calendar.getInstance()
        end.setTime(a.history._2)
        AdminTools.addAlias(a, start.getTime)(esClient)
        while (start.before(end)) {
          AdminTools.addAlias(a, start.getTime)(esClient)
          start.add(a.increaseType._2, 1)
        }
      }
    })
  }

  def loadTemplates(templates: List[String], force: Boolean, esClient: Client): Unit = {
    logger.info("Start loadTemplates")
    templates.foreach(t => {
      val file = new File(t)
      val old = esClient
        .admin()
        .indices()
        .prepareGetTemplates(file.getName)
        .execute()
        .actionGet()
        .getIndexTemplates
      if (old.isEmpty || force) {
        val content = GrantRun(() => FileUtils.readFileToByteArray(file))
        esClient
          .admin()
          .indices()
          .preparePutTemplate(file.getName)
          .setSource(content, XContentType.JSON)
          .execute()
          .actionGet()
      }
    })
  }
}
