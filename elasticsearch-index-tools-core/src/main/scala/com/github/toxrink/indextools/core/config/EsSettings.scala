package com.github.toxrink.indextools.core.config
import java.util

import org.elasticsearch.common.settings.Settings

/**
  * Created by xw on 2019/12/24.
  *
  * 每个版本settings里面的方法不一样可能需要重写
  * 不兼容的方法,在对应工程中继承此类重写对应方法
  * 默认基于6.1.3版本实现
  */
class EsSettings(val settings: Settings) {
  def getAsBoolean(key: String, default: Boolean): Boolean = {
    settings.getAsBoolean(key, default)
  }

  def getAsInt(key: String, default: Int): Int = {
    settings.getAsInt(key, default)
  }

  def get(key: String, default: String): String = {
    settings.get(key, default)
  }

  def get(key: String): String = {
    settings.get(key)
  }

  def getAsList(key: String): util.List[String] = {
    settings.getAsList(key)
  }

  def getByPrefix(prefix: String): Settings = {
    settings.getByPrefix(prefix)
  }

  def build(settings: Settings): EsSettings = {
    new EsSettings(settings)
  }
}
