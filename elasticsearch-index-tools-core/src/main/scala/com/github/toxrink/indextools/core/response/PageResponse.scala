package com.github.toxrink.indextools.core.response

import org.elasticsearch.rest.{RestRequest, RestStatus}

/**
  * Created by xw on 2019/9/4.
  */
case class PageResponse(r: RestRequest) extends ResourceResponse(r) {

  override def contentType(): String = "text/html; charset=utf-8"

  override def status(): RestStatus = s
}
