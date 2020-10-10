package com.github.toxrink.indextools.core.response

import org.elasticsearch.rest.{RestRequest, RestStatus}

/**
  * Created by xw on 2019/9/29.
  */
class OtherResponse(r: RestRequest) extends ResourceResponse(r) {

  override def contentType(): String = "application/octet-stream"

  override def status(): RestStatus = RestStatus.OK
}
