package com.github.toxrink.indextools.core.config

import java.io.File

import com.github.toxrink.indextools.core.constants.IndexToolsConstants
import com.github.toxrink.indextools.core.security.GrantRun
import com.github.toxrink.indextools.resource.ResourceMapping
import org.apache.commons.lang.StringUtils

/**
  * Created by xw on 2019/10/23.
  */
case class IndexToolsSettings(settings: EsSettings) {

  val autoCreate: AutoCreate = AutoCreate(settings)

  val securityConfig: SecurityConfig = SecurityConfig(settings)

  /**
    * 索引模板列表
    */
  val templates: List[String] = {
    val path = settings.get(IndexToolsConstants.ITOOLS_TEMPLATES)
    if (StringUtils.isNotEmpty(path)) {
      GrantRun(() => {
        val file = new File(path)
        if (file.exists()) {
          if (file.isFile) {
            List(file.getAbsolutePath)
          } else {
            file
              .listFiles()
              .map(_.getAbsolutePath)
              .filter(_.endsWith(".json"))
              .toList
          }
        } else {
          List[String]()
        }
      })
    } else {
      List[String]()
    }
  }

  /**
    * 自动创建索引检查时间
    */
  val checkInterval: String = settings.get(IndexToolsConstants.ITOOLS_AUTO_CHECK_TIME, "0 0 22 * * ?")

  /**
    * 自动创建测试数据检查时间
    */
  val testDataInterval: String = settings.get(IndexToolsConstants.ITOOLS_AUTO_CREATE_TESTDATA_CHECK_TIME, "0 0 6 * * ?")

  val resourceDir: String = settings.get(IndexToolsConstants.ITOOLS_RESOURCE, "")

  {
    ResourceMapping.resourceDir = resourceDir
  }
}
