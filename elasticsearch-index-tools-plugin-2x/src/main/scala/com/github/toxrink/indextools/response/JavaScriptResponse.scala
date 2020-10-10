package com.github.toxrink.indextools.response

import org.elasticsearch.rest.RestRequest

/**
  * Created by xw on 2019/9/5.
  */
case class JavaScriptResponse(r: RestRequest) extends ResourceResponse2x(r) {

  override def contentType(): String = "application/javascript"

}
