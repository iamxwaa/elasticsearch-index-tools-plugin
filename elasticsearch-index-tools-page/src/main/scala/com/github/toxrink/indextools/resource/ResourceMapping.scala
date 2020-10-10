package com.github.toxrink.indextools.resource

import java.io.File

import org.apache.commons.lang.StringUtils

/**
  * Created by xw on 2019/12/16.
  */
object ResourceMapping {
  var resourceDir = ""

  //key->(路径,是否缓存)
  var securitySourceMap = Map(
    "/_itools" -> ("/index.html", true)
  )

  def sourceMap: Map[String, (String, Boolean)] = {
    securitySourceMap
  }

  def getSource(path: String): Option[(String, Boolean)] = {
    if (StringUtils.isEmpty(resourceDir)) {
      sourceMap.get(path)
    } else {
      val p = sourceMap.get(path)
      if (p.isDefined) {
        if (exist(resourceDir + p.get._1)) {
          Some((resourceDir + p.get._1, false))
        } else {
          p
        }
      } else {
        if (exist(resourceDir + path)) {
          Some((resourceDir + path, false))
        } else {
          Some((path, true))
        }
      }
    }
  }

  private def exist(path: String): Boolean = {
    GrantApply(() => {
      val file = new File(path)
      val e = file.exists()
      e
    })
  }
}
