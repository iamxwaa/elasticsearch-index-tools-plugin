
package com.github.toxrink.indextools.core.response

import java.util

import com.github.toxrink.indextools.core.security.GrantRun
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestResponse, RestStatus}

/**
  * Created by xw on 2019/12/18.
  */
case class JsonResponse(data: util.Map[String, Object]) extends RestResponse {

  override def contentType(): String = "application/json; charset=utf-8"

  override def content(): BytesReference = {
    val content = GrantRun.toJSON(data)
    new BytesArray(content)
  }

  override def status(): RestStatus = RestStatus.OK
}
