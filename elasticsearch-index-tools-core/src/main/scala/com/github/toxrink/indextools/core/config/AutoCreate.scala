package com.github.toxrink.indextools.core.config

import com.github.toxrink.indextools.core.constants.IndexToolsConstants._
import x.utils.TimeUtils

/**
  * Created by xw on 2019/10/23.
  */
case class AutoCreate(settings: EsSettings) {
  val open: Boolean = settings.getAsBoolean(ITOOLS_AUTO_CREATE_OPEN, false)

  val autoCreateIndices: List[AutoCreateIndex] = {
    import scala.collection.JavaConversions._
    val psettings = settings.build(settings.getByPrefix(ITOOLS_AUTO_CREATE_INDEX_PREFIX))
    settings
      .getAsList(ITOOLS_AUTO_CREATE_AVALIABLE_INDEX)
      .map(number => {
        val name = psettings.get(s"$number.$ITOOLS_AUTO_CREATE_INDEX_NAME")
        val nameFormat = psettings.get(s"$number.$ITOOLS_AUTO_CREATE_INDEX_NAME_FORMAT", NAME_FORMAT)
        val format = psettings.get(s"$number.$ITOOLS_AUTO_CREATE_INDEX_FORMAT")
        val aliasFormat = psettings.get(s"$number.$ITOOLS_AUTO_CREATE_INDEX_ALIAS_FORMAT")
        val timeField = psettings.get(s"$number.$ITOOLS_AUTO_CREATE_INDEX_TIME_FIELD", TIME_FIELD)
        val createHistory = psettings.getAsList(s"$number.$ITOOLS_AUTO_CREATE_INDEX_CREATE_HISTORY")
        if (createHistory.isEmpty) {
          AutoCreateIndex(name, nameFormat, format, aliasFormat, timeField, null)
        } else {
          val start = TimeUtils.parese(createHistory(0), format)
          val end = TimeUtils.parese(createHistory(1), format)
          AutoCreateIndex(name, nameFormat, format, aliasFormat, timeField, (start, end))
        }
      })
      .toList
  }
}
