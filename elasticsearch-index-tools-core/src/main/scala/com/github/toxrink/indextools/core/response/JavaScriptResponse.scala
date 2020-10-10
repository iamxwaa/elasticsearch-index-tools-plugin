package com.github.toxrink.indextools.core.response

import org.elasticsearch.rest.{RestRequest, RestStatus}

/**
  * Created by xw on 2019/9/5.
  */
case class JavaScriptResponse(r: RestRequest) extends ResourceResponse(r) {

  override def contentType(): String = "application/javascript"

  override def status(): RestStatus = s
}
