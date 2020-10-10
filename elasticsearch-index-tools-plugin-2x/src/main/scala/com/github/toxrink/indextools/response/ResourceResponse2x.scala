package com.github.toxrink.indextools.response

import com.github.toxrink.indextools.core.response.ResourceResponse
import org.elasticsearch.common.bytes.BytesReference
import org.elasticsearch.rest.{RestRequest, RestStatus}

/**
  * Created by xw on 2019/9/5.
  */
abstract class ResourceResponse2x(r: RestRequest) extends ResourceResponse(r) {
  private var tmpContent: BytesReference = _

  override def status(): RestStatus = {
    if (null == s) {
      tmpContent = content()
    }
    s
  }

}
