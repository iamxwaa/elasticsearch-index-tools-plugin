package com.github.toxrink.indextools.response

import org.elasticsearch.rest.RestRequest

/**
  * Created by xw on 2019/9/4.
  */
case class PageResponse(r: RestRequest) extends ResourceResponse2x(r) {

  override def contentType(): String = "text/html; charset=utf-8"

}
