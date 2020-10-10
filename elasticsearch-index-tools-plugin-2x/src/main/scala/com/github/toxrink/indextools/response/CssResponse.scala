package com.github.toxrink.indextools.response

import org.elasticsearch.rest.RestRequest

/**
  * Created by xw on 2019/9/5.
  */
case class CssResponse(r: RestRequest) extends ResourceResponse2x(r) {

  override def contentType(): String = "text/css"

}
