package com.github.toxrink.indextools.core.response

import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestResponse, RestStatus}

/**
  * Created by xw on 2019/9/4.
  */
case class UnknownResponse() extends RestResponse {

  addHeader("Access-Control-Allow-Origin", "*")

  override def contentType(): String = "application/json; charset=utf-8"

  override def content(): BytesReference = {
    new BytesArray("{\"status\":\"Unknow\"}")
  }

  override def status(): RestStatus = RestStatus.BAD_REQUEST
}
