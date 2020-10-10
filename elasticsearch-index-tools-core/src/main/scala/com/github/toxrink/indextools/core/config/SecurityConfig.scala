package com.github.toxrink.indextools.core.config

import com.github.toxrink.indextools.core.constants.IndexToolsConstants._
import com.github.toxrink.indextools.core.model.{AdminPermission, DataPermission, UserPermission}
import com.github.toxrink.indextools.core.ssl.HttpSSLContextFactory
import javax.net.ssl.SSLContext

/**
  * Created by xw on 2019/10/23.
  */
object SecurityConfig {
  val defaultAdminPermission: AdminPermission = AdminPermission(create = false, delete = false)
  val defaultDataPermission: DataPermission = DataPermission(write = false, read = false)
}

case class SecurityConfig(esSettings: EsSettings) {
  val basicEnable: Boolean = esSettings.getAsBoolean(ITOOLS_SECURITY_BASIC, false)

  val httpSSLEnable: Boolean = esSettings.getAsBoolean(ITOOLS_SECURITY_HTTP_SSLONLY, false)

  val httpKeystorePath: Option[String] = Option(esSettings.get(ITOOLS_SECURITY_HTTP_KEYSTORE, null))

  val httpKeystorePassword: Option[String] = Option(esSettings.get(ITOOLS_SECURITY_HTTP_KEYSTORE_PASSWORD, null))

  val httpCertificatePassword: Option[String] = Option(
    esSettings.get(ITOOLS_SECURITY_HTTP_CERTIFICATE_PASSWORD, null))

  val httpAlgorithm: String = esSettings.get(ITOOLS_SECURITY_HTTP_ALGORITHM, "SunX509")

  val httpSSLProtocol: String = esSettings.get(ITOOLS_SECURITY_HTTP_PROTOCOL, "TLSv1.2")

  val superUser: String = esSettings.get(ITOOLS_SECURITY_USERNAME, "itools")

  val superPassword: String = esSettings.get(ITOOLS_SECURITY_PASSWORD, "indextools")

  val superAdmin: UserPermission = UserPermission(
    superAdmin = true,
    superUser,
    superPassword,
    SecurityConfig.defaultAdminPermission,
    SecurityConfig.defaultDataPermission,
    List(),
    allowAllIndex = true
  )

  val httpSSLContext: Option[SSLContext] =
    if (httpSSLEnable) Some(HttpSSLContextFactory.createSSLContext(this)) else None

  def isSuperAdmin(name: String, password: String): Option[UserPermission] = {
    if (superAdmin.name.equals(name) &&
        superAdmin.password.equals(password)) {
      Some(superAdmin)
    } else {
      None
    }
  }
}
