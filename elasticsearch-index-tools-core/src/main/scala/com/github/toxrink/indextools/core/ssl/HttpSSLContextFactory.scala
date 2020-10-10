package com.github.toxrink.indextools.core.ssl

import java.security.cert.X509Certificate
import java.security.{KeyStore, Security}

import com.github.toxrink.indextools.core.config.SecurityConfig
import com.github.toxrink.indextools.core.security.GrantRun
import javax.net.ssl._
import x.utils.JxUtils

/**
  * Created by xw on 2019/10/23.
  */
object HttpSSLContextFactory {
  private val logger = JxUtils.getLogger(HttpSSLContextFactory.getClass)

  case class ItoolsTrustManager() extends X509TrustManager {
    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

    override def getAcceptedIssuers: Array[X509Certificate] = null
  }

  val defaultItoolsTrustManagers = Array[TrustManager](ItoolsTrustManager())

  def createSSLContext(securityConfig: SecurityConfig): SSLContext = {
    val serverSSLContext = GrantRun(() => {
      var algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm")
      if (algorithm == null) {
        algorithm = securityConfig.httpAlgorithm
      }

      logger.debug("Load HttpKeyStore")
      val httpsKeyStore = HttpsKeyStore(securityConfig)
      val ks: KeyStore = KeyStore.getInstance("JKS")
      ks.load(httpsKeyStore.getHttpKeyStoreStream(), httpsKeyStore.getHttpKeyStorePassword())
      val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(algorithm)
      kmf.init(ks, httpsKeyStore.getHttpCertificatePassword())

      logger.debug("Create Server SSLContext")
      val sslContext = SSLContext.getInstance(securityConfig.httpSSLProtocol)
      sslContext.init(kmf.getKeyManagers(), null, null)
      sslContext
    })
    checkClientSSLContext(securityConfig)
    serverSSLContext
  }

  def checkClientSSLContext(securityConfig: SecurityConfig): Unit = {
    GrantRun(() => {
      logger.debug("Create Client SSLContext")
      val sslContext = SSLContext.getInstance(securityConfig.httpSSLProtocol)
      sslContext.init(null, defaultItoolsTrustManagers, null)

      val ignoreHostnameVerifier = new HostnameVerifier() {
        def verify(s: String, sslsession: SSLSession): Boolean = {
          true
        }
      }
      HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostnameVerifier)
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory())
    })
  }
}
