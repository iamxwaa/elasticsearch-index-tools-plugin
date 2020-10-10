package com.github.toxrink.indextools.resource

import java.io.{ByteArrayOutputStream, File, FileInputStream, InputStream}

import org.apache.commons.io.IOUtils

/**
  * Created by xw on 2019/12/16.
  *<br>
  * type A :org.elasticsearch.rest.RestStatus
  *<br>
  *  type B :org.elasticsearch.common.bytes.BytesArray
  */
class ResourceCache[A, B] {

  /**
    * 缓存静态文件
    */
  private var resourceCache = Map[String, (A, B)]()

  /**
    * 返回静态文件缓存
    * @return
    */
  def cacheFile: Map[String, (A, B)] = {
    resourceCache
  }

  /**
    * 新增静态文件到缓存
    * @param cache 新增缓存
    */
  def addCacheFile(cache: Map[String, (A, B)]): Unit = {
    resourceCache = resourceCache ++ cache
  }

  /**
    * 获取文件数据
    * @param localPath 本地文件路径
    * @param jarPath jar包中的文件路径
    * @return
    */
  def loadResource(localPath: String, jarPath: String): Array[Byte] = {
    GrantApply(() => {
      val file = new File(localPath)
      if (file.exists()) {
        readStream(new FileInputStream(file))
      } else {
        val stream = this.getClass.getClassLoader.getResourceAsStream("_site" + jarPath)
        readStream(stream)
      }
    })
  }

  /**
    * 获取文件数据
    * @param sitePath site目录下的路径
    * @return
    */
  def loadResource(sitePath: String): Array[Byte] = {
    readStream(this.getClass.getClassLoader.getResourceAsStream("_site" + sitePath))
  }

  private def readStream(input: InputStream): Array[Byte] = {
    if (null == input) {
      Array()
    } else {
      val output = new ByteArrayOutputStream()
      IOUtils.copy(input, output)
      IOUtils.closeQuietly(input)
      val arr = output.toByteArray
      IOUtils.closeQuietly(output)
      arr
    }
  }
}
