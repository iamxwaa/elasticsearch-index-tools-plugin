package com.github.toxrink.indextools.ssl

import com.github.toxrink.indextools.IndexToolsSecurityPlugin
import io.netty.channel.{Channel, ChannelHandler}
import io.netty.handler.ssl.SslHandler
import org.elasticsearch.common.network.NetworkService
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.util.BigArrays
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.http.netty4.Netty4HttpServerTransport
import org.elasticsearch.http.{HttpHandlingSettings, HttpServerTransport}
import org.elasticsearch.threadpool.ThreadPool

/**
  * Created by xw on 2019/10/23.
  */
class Netty4HttpsNettyTransport(settings: Settings,
                                networkService: NetworkService,
                                bigArrays: BigArrays,
                                threadPool: ThreadPool,
                                xContentRegistry: NamedXContentRegistry,
                                dispatcher: HttpServerTransport.Dispatcher,
                                indexToolsSecurityPlugin: IndexToolsSecurityPlugin)
    extends Netty4HttpServerTransport(
      settings: Settings,
      networkService: NetworkService,
      bigArrays: BigArrays,
      threadPool: ThreadPool,
      xContentRegistry: NamedXContentRegistry,
      dispatcher: HttpServerTransport.Dispatcher
    ) {

  override def configureServerChannelHandler(): ChannelHandler = {
    new SSLHttpChannelHandler(this, this.handlingSettings)
  }

  class SSLHttpChannelHandler(transport: Netty4HttpServerTransport, handlingSettings: HttpHandlingSettings)
      extends Netty4HttpServerTransport.HttpChannelHandler(transport: Netty4HttpServerTransport,
                                                           handlingSettings: HttpHandlingSettings) {
    override def initChannel(ch: Channel): Unit = {
      val sconfig = indexToolsSecurityPlugin.getIndexToolsSettings.securityConfig
      if (sconfig.httpSSLContext.isDefined) {
        val sslEngine = sconfig.httpSSLContext.get.createSSLEngine()
        sslEngine.setUseClientMode(false)
        sslEngine.setNeedClientAuth(false)
        sslEngine.setWantClientAuth(false)
        val sslHandler = new SslHandler(sslEngine)
        ch.pipeline().addLast("sslHandler", sslHandler)
      }
      super.initChannel(ch)
    }
  }
}
