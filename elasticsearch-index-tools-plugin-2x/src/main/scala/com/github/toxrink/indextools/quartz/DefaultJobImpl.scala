package com.github.toxrink.indextools.quartz

import java.util

import com.github.toxrink.indextools.IndexToolsPlugin
import com.github.toxrink.indextools.config.EsSettings2x
import com.github.toxrink.indextools.core.config.AutoCreate
import com.github.toxrink.indextools.core.constants.IndexToolsConstants._
import com.github.toxrink.indextools.tools.AdminTools
import org.elasticsearch.common.settings.Settings
import org.quartz.{Job, JobExecutionContext}

/**
  * Created by xw on 2019/9/20.
  */
class DefaultJobImpl() extends Job {

  override def execute(context: JobExecutionContext): Unit = {
    AdminTools.preCreateIndex(IndexToolsPlugin.getIndexToolsSettings.autoCreate.autoCreateIndices,
                              IndexToolsPlugin.getEsClient)
    val exist = IndexToolsPlugin.getEsClient
      .admin()
      .indices()
      .prepareExists(AUTO_CREATE_INDEX_NAME)
      .execute()
      .actionGet()
    if (exist.isExists) {
      val response = IndexToolsPlugin.getEsClient
        .prepareSearch(AUTO_CREATE_INDEX_NAME)
        .setSize(1000)
        .execute()
        .actionGet()

      val builder = Settings.builder()
      val list = new util.ArrayList[String]()
      var index = 1
      response.getHits.getHits.foreach(hit => {
        val map = hit.getSource
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
      builder.putArray(ITOOLS_AUTO_CREATE_AVALIABLE_INDEX, list.toArray(Array.empty[String]): _*)
      builder.put(ITOOLS_AUTO_CREATE_OPEN, true)
      val auto2 = AutoCreate(new EsSettings2x(builder.build()))
      AdminTools.preCreateIndex(auto2.autoCreateIndices, IndexToolsPlugin.getEsClient)
    }
  }

}
