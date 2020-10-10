package com.github.toxrink.indextools.core.ssl

import java.io.{FileInputStream, InputStream}

import com.github.toxrink.indextools.core.config.SecurityConfig

/**
  * Created by xw on 2019/10/23.
  */
case class HttpsKeyStore(securityConfig: SecurityConfig) {
  def getHttpKeyStoreStream(): InputStream = {
    new FileInputStream(securityConfig.httpKeystorePath.get)
  }

  def getHttpCertificatePassword(): Array[Char] = {
    securityConfig.httpCertificatePassword.get.toCharArray
  }

  def getHttpKeyStorePassword(): Array[Char] = {
    securityConfig.httpKeystorePassword.get.toCharArray
  }
}
