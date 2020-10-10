package com.github.toxrink.indextools.core.security

import com.github.toxrink.indextools.core.config.IndexToolsSettings
import com.github.toxrink.indextools.core.constants.IndexToolsConstants
import com.github.toxrink.indextools.core.model.UserPermission
import org.elasticsearch.common.util.concurrent.ThreadContext

/**
  * Created by xw on 2019/10/21.
  */
class SecurityAction(threadContext: ThreadContext, indexToolsSettings: IndexToolsSettings, userCheck: UserCheck) {

  def securityConfig = indexToolsSettings.securityConfig

  def checkUserPermission(name: String, password: String): Option[UserPermission] =
    userCheck.checkUserPermission(name, password)

  def isSuperAdmin(name: String, password: String): Option[UserPermission] = {
    indexToolsSettings.securityConfig.isSuperAdmin(name, password)
  }

  def putTransientPermission(userPermission: UserPermission): Unit = {
    threadContext.putTransient(IndexToolsConstants.USER_PERMISSION, userPermission)
  }

  def getTransientPermission(): Option[UserPermission] = {
    val userPermission: UserPermission = threadContext.getTransient(IndexToolsConstants.USER_PERMISSION)
    Option(userPermission)
  }

  def setContextUser(userPermission: Option[UserPermission]): Unit = {
    val test = getContextUser()
    if (userPermission.isDefined && test.isEmpty) {
      threadContext.putHeader(IndexToolsConstants.USER_PERMISSION_USERNAME, userPermission.get.name)
      threadContext.putHeader(IndexToolsConstants.USER_PERMISSION_PASSWORD, userPermission.get.password)
    }
  }

  def getContextUser(): Option[UserPermission] = {
    val name = threadContext.getHeader(IndexToolsConstants.USER_PERMISSION_USERNAME)
    val password = threadContext.getHeader(IndexToolsConstants.USER_PERMISSION_PASSWORD)
    if (null == name && null == password) {
      None
    } else {
      val superAdmin = isSuperAdmin(name, password)
      if (superAdmin.isDefined) {
        superAdmin
      } else {
        checkUserPermission(name, password)
      }
    }
  }
}
