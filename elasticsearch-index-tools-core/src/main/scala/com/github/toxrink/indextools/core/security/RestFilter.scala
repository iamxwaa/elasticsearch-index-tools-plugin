package com.github.toxrink.indextools.core.security

import com.github.toxrink.indextools.core.constants.PermissionConstants
import com.github.toxrink.indextools.core.response.{BAResponse, CheckResponse}
import org.elasticsearch.client.node.NodeClient
import org.elasticsearch.rest._
import x.utils.StrUtils

/**
  * Created by xw on 2019/10/12.
  */
class RestFilter(securityAction: SecurityAction) {

  def wrap(original: RestHandler): RestHandler = {
    new RestHandler() {
      override def handleRequest(request: RestRequest, channel: RestChannel, client: NodeClient): Unit = {
        val check = checkRequestPermission(request)
        if (check.isRight) {
          original.handleRequest(request, channel, client)
        } else {
          channel.sendResponse(check.left.get)
        }
      }
    }
  }

  def checkRequestPermission(request: RestRequest): Either[RestResponse, Boolean] = {
    if (securityAction.securityConfig.basicEnable) {
      val basic = request.header("Authorization")
      if (null != basic && basic.startsWith("Basic")) {
        val ba = basic.substring(6)
        val userAndPass = {
          val pass = StrUtils.decodeBase64(ba)
          val idx = pass.indexOf(":")
          (pass.substring(0, idx), pass.substring(idx + 1))
        }
        val account = userAndPass._1
        val password = userAndPass._2
        val check = checkRequestUser(account, password)
        if (check._1) {
          Right(true)
        } else {
          Left(CheckResponse(check._3, check._2))
        }
      } else {
        Left(BAResponse())
      }
    } else {
      Right(true)
    }
  }

  def checkRequestUser(account: String, password: String): (Boolean, RestStatus, String) = {
    val superAdmin = securityAction.isSuperAdmin(account, password)
    if (superAdmin.isDefined) {
      securityAction.putTransientPermission(superAdmin.get)
      securityAction.setContextUser(superAdmin)
      (true, RestStatus.OK, "")
    } else {
      val permission = securityAction.checkUserPermission(account, StrUtils.toMD5(password))
      if (permission.isDefined) {
        securityAction.putTransientPermission(permission.get)
        securityAction.setContextUser(permission)
        (true, RestStatus.OK, "")
      } else {
        (false, RestStatus.FORBIDDEN, PermissionConstants.WRONG_RIGHT)
      }
    }
  }
}
