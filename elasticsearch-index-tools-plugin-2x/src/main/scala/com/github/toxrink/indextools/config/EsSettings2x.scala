package com.github.toxrink.indextools.config

import java.util

import com.github.toxrink.indextools.core.config.EsSettings
import org.elasticsearch.common.settings.Settings

/**
  * Created by xw on 2019/12/25.
  */
class EsSettings2x(override val settings: Settings) extends EsSettings(settings) {
  override def getAsList(key: String): util.List[String] = {
    util.Arrays.asList(settings.getAsArray(key): _*)
  }

  override def build(settings: Settings): EsSettings = {
    new EsSettings2x(settings)
  }
}
