package com.github.toxrink.indextools.core.response

import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestResponse, RestStatus}

/**
  * Created by xw on 2019/10/14.
  */
case class CheckResponse(message: String, st: RestStatus) extends RestResponse {

  addHeader("Access-Control-Allow-Origin", "*")

  override def contentType(): String = {
    "application/json; charset=utf-8"
  }

  override def content(): BytesReference = {
    new BytesArray(s"""{"status":${st.getStatus},"error":"$message"}""")
  }

  override def status(): RestStatus = st
}
