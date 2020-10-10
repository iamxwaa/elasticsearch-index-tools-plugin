package com.github.toxrink.indextools.core.response

import java.util

import com.github.toxrink.indextools.core.security.GrantRun
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestResponse, RestStatus};

/**
  * Created by xw on 2019/9/4.
  */
case class RestOkResponse(message: String) extends RestResponse {

  def this(data: util.Map[String, Object]) = this(GrantRun.toJSON(data))

  addHeader("Access-Control-Allow-Origin", "*")

  def this() = {
    this("{\"status\":\"OK\"}")
  }

  override def contentType(): String = {
    if (message.startsWith("{") && message.endsWith("}")) {
      "application/json; charset=utf-8"
    } else {
      "text/plain; charset=utf-8"
    }
  }

  override def content(): BytesReference = {
    new BytesArray(message)
  }

  override def status(): RestStatus = RestStatus.OK
}
