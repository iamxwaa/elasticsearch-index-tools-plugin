package com.github.toxrink.indextools.core.tools

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import java.security.cert

import com.github.toxrink.indextools.core.config.SecurityConfig
import com.github.toxrink.indextools.core.security.GrantRun
import javax.net.ssl.X509TrustManager
import org.apache.commons.io.IOUtils
import x.utils.StrUtils

/**
  * Created by xw on 2019/10/22.
  */
object HttpTools {

  case class IToolsTrustStrategy() extends X509TrustManager {
    override def checkClientTrusted(x509Certificates: Array[cert.X509Certificate], s: String): Unit = {}

    override def checkServerTrusted(x509Certificates: Array[cert.X509Certificate], s: String): Unit = {}

    override def getAcceptedIssuers: Array[cert.X509Certificate] = null
  }

  def sendAdminGetRequest(host: String, port: Int, path: String)(implicit securityConfig: SecurityConfig): String = {
    GrantRun(() => {
      val queryString =
        if (securityConfig.httpSSLEnable)
          new StringBuffer("https://")
        else
          new StringBuffer("http://")
      queryString.append(host)
      queryString.append(":")
      queryString.append(port)
      queryString.append(path)

      val tmpURL = new URL(queryString.toString())
      val conn: HttpURLConnection = tmpURL.openConnection().asInstanceOf[HttpURLConnection]

      conn.setRequestMethod("GET")
      conn.setConnectTimeout(30 * 1000)
      conn.setReadTimeout(30 * 1000)
      if (securityConfig.basicEnable) {
        conn.setRequestProperty(
          "Authorization",
          "Basic " + (StrUtils.encodeBase64(securityConfig.superUser + ":" + securityConfig.superPassword)))
      }
      conn.connect()

      val resultBuffer = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))

      var tmp: String = resultBuffer.readLine()
      val result = new StringBuilder()
      while (tmp != null) {
        result.append(tmp)
        tmp = resultBuffer.readLine()
      }
      IOUtils.closeQuietly(resultBuffer)
      result.toString()
    })
  }
}
