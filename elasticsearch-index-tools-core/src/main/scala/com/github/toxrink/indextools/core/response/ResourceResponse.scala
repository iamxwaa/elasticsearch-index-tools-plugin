package com.github.toxrink.indextools.core.response

import com.github.toxrink.indextools.core.resource.ResourceLoader
import com.github.toxrink.indextools.resource.ResourceMapping
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestRequest, RestResponse, RestStatus}

/**
  * Created by xw on 2019/9/5.
  */
abstract class ResourceResponse(r: RestRequest) extends RestResponse {
  var s: RestStatus = _

  type SBI = ((String, Boolean), Array[Byte])

  addHeader("Access-Control-Allow-Origin", "*")

  override def content(): BytesReference = {
    if (ResourceLoader.cacheFile.contains(r.path())) {
      val a = ResourceLoader.cacheFile(r.path())
      s = a._1
      a._2
    } else {
      bytesArray(getResourceAsStream)
    }
  }

  private def getResourceAsStream: SBI = {
    val path = ResourceMapping.getSource(r.path())
    var path2 = ("", false)
    var is: Array[Byte] = null
    if (path.isDefined) {
      path2 = path.get
      is = ResourceLoader.loadResource(path.get._1, path.get._1)
    } else {
      path2 = (r.path(), false)
      is = ResourceLoader.loadResource("", r.path())
    }
    (path2, is)
  }

  private def bytesArray(sbi: SBI): BytesArray = {
    if (null == sbi._2) {
      s = RestStatus.NOT_FOUND
      cache(sbi, BytesArray.EMPTY)
    } else {
      s = RestStatus.OK
      cache(sbi, new BytesArray(sbi._2))
    }
  }

  private def cache(key: SBI, v: BytesArray): BytesArray = {
    if (key._1._2) {
      ResourceLoader.addCacheFile(Map(key._1._1 -> (s, v)))
    }
    v
  }

}
