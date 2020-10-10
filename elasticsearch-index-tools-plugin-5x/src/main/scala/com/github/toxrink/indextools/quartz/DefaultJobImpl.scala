package com.github.toxrink.indextools.quartz

import java.util

import com.github.toxrink.indextools.config.EsSettings5x
import com.github.toxrink.indextools.core.config.AutoCreate
import com.github.toxrink.indextools.core.constants.IndexToolsConstants._
import com.github.toxrink.indextools.tools.AdminTools
import com.github.toxrink.indextools.{IndexToolsPlugin, IndexToolsSecurityPlugin}
import org.elasticsearch.common.settings.Settings
import org.quartz.{Job, JobExecutionContext}

/**
  * Created by xw on 2019/9/20.
  */
class DefaultJobImpl() extends Job {

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
    AdminTools.preCreateIndex(indexTools.getIndexToolsSettings.autoCreate.autoCreateIndices, indexTools.getEsClient)
    val exist = indexTools.getEsClient
      .admin()
      .indices()
      .prepareExists(AUTO_CREATE_INDEX_NAME)
      .execute()
      .actionGet()
    if (exist.isExists) {
      val response = indexTools.getEsClient
        .prepareSearch(AUTO_CREATE_INDEX_NAME)
        .setSize(1000)
        .execute()
        .actionGet()

      val builder = Settings.builder()
      val list = new util.ArrayList[String]()
      var index = 1
      response.getHits.getHits.foreach(hit => {
        val map = hit.getSourceAsMap
        val name = map.get("name").toString
        val nameFormat = map.get("nameFormat").toString
        val format = map.get("format").toString
        val aliasFormat = map.get("aliasFormat").toString
        val timeField = map.get("timeField").toString

        builder.put(s"$ITOOLS_AUTO_CREATE_INDEX_PREFIX$index.$ITOOLS_AUTO_CREATE_INDEX_NAME", name)
        builder.put(s"$ITOOLS_AUTO_CREATE_INDEX_PREFIX$index.$ITOOLS_AUTO_CREATE_INDEX_NAME_FORMAT", nameFormat)
        builder.put(s"$ITOOLS_AUTO_CREATE_INDEX_PREFIX$index.$ITOOLS_AUTO_CREATE_INDEX_FORMAT", format)
        builder.put(s"$ITOOLS_AUTO_CREATE_INDEX_PREFIX$index.$ITOOLS_AUTO_CREATE_INDEX_ALIAS_FORMAT", aliasFormat)
        builder.put(s"$ITOOLS_AUTO_CREATE_INDEX_PREFIX$index.$ITOOLS_AUTO_CREATE_INDEX_TIME_FIELD", timeField)
        list.add(index.toString)
        index = index + 1
      })
      builder.putArray(ITOOLS_AUTO_CREATE_AVALIABLE_INDEX, list)
      builder.put(ITOOLS_AUTO_CREATE_OPEN, true)
      val auto2 = AutoCreate(new EsSettings5x(builder.build()))
      AdminTools.preCreateIndex(auto2.autoCreateIndices, indexTools.getEsClient)
    }
  }

}
