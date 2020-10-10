package com.github.toxrink.indextools.core.resource

import com.github.toxrink.indextools.resource.ResourceCache
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.rest.RestStatus

/**
  * Created by xw on 2019/9/10.
  */
object ResourceLoader {
  private val resourceCache = new ResourceCache[RestStatus, BytesArray]

  def cacheFile = {
    resourceCache.cacheFile
  }

  def addCacheFile(cache: Map[String, (RestStatus, BytesArray)]): Unit = {
    resourceCache.addCacheFile(cache)
  }

  def loadResource(localPath: String, jarPath: String): Array[Byte] = {
    resourceCache.loadResource(localPath, jarPath)
  }

  def loadResource(sitePath: String): Array[Byte] = {
    resourceCache.loadResource(sitePath)
  }

}
