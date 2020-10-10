package com.github.toxrink.indextools.quartz

import java.util

import com.github.toxrink.indextools.core.constants.IndexToolsConstants._
import com.github.toxrink.indextools.rest.TestDataCreateHandler
import com.github.toxrink.indextools.{IndexToolsPlugin, IndexToolsSecurityPlugin}
import org.elasticsearch.search.SearchHit
import org.quartz.{Job, JobExecutionContext}
import x.utils.{JxUtils, TimeUtils}

/**
  * Created by cz on 2019/12/24.
  */
class TestDataJobImpl() extends Job {

  private val LOG = JxUtils.getLogger(classOf[TestDataJobImpl])

  override def execute(context: JobExecutionContext): Unit = {
    val indexTools =
      context.getJobDetail.getJobDataMap.get(INDEX_CREATE_JOB).asInstanceOf[IndexToolsPlugin]
    if (indexTools.getIndexToolsSettings.securityConfig.basicEnable) {
      indexTools match {
        case it: IndexToolsSecurityPlugin =>
          val admin = indexTools.getIndexToolsSettings.securityConfig.superAdmin
          it.getSecurityAction.setContextUser(Some(admin))
      }
    }
    val esClient = indexTools.getEsClient

    val searchResponse = esClient.prepareSearch(INDEX_GENERATETEST_TEMPLATE).setSize(1000).get

    searchResponse.getHits.getHits.foreach((hit:SearchHit) => {
      val param = new util.LinkedHashMap[String, Object]
      param.put("date", TimeUtils.format(TimeUtils.getNowBeforeByDay(1),"yyyy-MM-dd"));
      val indexSuffix = TimeUtils.format(TimeUtils.getNowBeforeByDay(1),"-yyyy.MM")
      val map = hit.getSourceAsMap
      val isCron= java.lang.Boolean.valueOf(map.getOrDefault("cron","false").toString)
      param.put("rows", map.getOrDefault("cron_rows","100"))

      if(!isCron) return
      param.put("index", hit.getId + indexSuffix)
      param.put("template", map)
      LOG.info(s"自动创建${param.get("index")}索引测试数据~~~~~~~~~~")
      TestDataCreateHandler.generateData(param, esClient)
    })

  }

}
