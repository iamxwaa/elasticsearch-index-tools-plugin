package com.github.toxrink.indextools.core.response

import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestResponse, RestStatus}

/**
  * Created by xw on 2019/10/14.
  */
case class BAResponse() extends RestResponse {

  addHeader("Access-Control-Allow-Origin", "*")
  addHeader("WWW-Authenticate", "Basic Realm=\"Index-Tools\"")

  override def contentType(): String = {
    "text/plain; charset=utf-8"
  }

  override def content(): BytesReference = {
    new BytesArray("")
  }

  override def status(): RestStatus = RestStatus.UNAUTHORIZED
}
