package com.github.toxrink.indextools;

import com.github.toxrink.indextools.config.EsSettings2x;
import com.github.toxrink.indextools.core.config.EsSettings;
import com.github.toxrink.indextools.core.config.IndexToolsSettings;
import com.github.toxrink.indextools.core.constants.IndexToolsConstants;
import com.github.toxrink.indextools.core.quartz.EsScheduler;
import com.github.toxrink.indextools.core.security.GrantRun;
import com.github.toxrink.indextools.quartz.DefaultJobImpl;
import com.github.toxrink.indextools.rest.AutoCreateHandler;
import com.github.toxrink.indextools.rest.CommandHandler;
import com.github.toxrink.indextools.rest.ResourceHandler;
import com.github.toxrink.indextools.tools.AdminTools;
import org.apache.commons.logging.Log;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;
import org.quartz.JobDataMap;
import x.utils.JxUtils;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by xw on 2019/9/18.
 */
public class IndexToolsPlugin extends Plugin {

    private static final Log LOG = JxUtils.getLogger(IndexToolsPlugin.class);

    protected EsSettings esSettings;

    private static IndexToolsPlugin indexToolsPlugin;

    protected Client esClient;
    protected IndexToolsSettings indexToolsSettings;
    protected EsScheduler esScheduler;

    @Inject
    public IndexToolsPlugin(Settings settings) {
        esSettings = new EsSettings2x(settings);
        indexToolsSettings = new IndexToolsSettings(esSettings);
        this.esClient = getClient(settings);
        this.indexToolsPlugin = this;
        onNodeStarted();
    }

    public void onNodeStarted() {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            AdminTools.loadTemplates(indexToolsSettings.templates(), false, esClient);
            if (indexToolsSettings.autoCreate().open()) {
                AdminTools.preCreateIndex(indexToolsSettings.autoCreate().autoCreateIndices(), esClient);
            }
            loadScheduler();
            if (null != esScheduler) {
                LOG.info("Start index auto create scheduler");
                esScheduler.start();
            }
        }, 20, TimeUnit.SECONDS);
    }

    protected void loadScheduler() {
        if (null != esScheduler) {
            esScheduler.stop();
            esScheduler.clear();
            esScheduler = null;
        }
        //加载索引自动创建
        if (indexToolsSettings.autoCreate().open()) {
            esScheduler = GrantRun.applyJava(() -> {
                EsScheduler esScheduler2 = new EsScheduler();

                final Properties props = new Properties();
                props.put("org.quartz.scheduler.instanceName", "static-index-tools");
                props.put("org.quartz.threadPool.threadCount", "2");

                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put(IndexToolsConstants.INDEX_CREATE_JOB, this);

                esScheduler2.init(props);
                esScheduler2.addSchedule("static-index-tools"
                        , indexToolsSettings.checkInterval()
                        , DefaultJobImpl.class, jobDataMap);

                return esScheduler2;
            });
        }
    }

    public void onModule(RestModule module) {
        module.addRestAction(CommandHandler.class);
        module.addRestAction(ResourceHandler.class);
        module.addRestAction(AutoCreateHandler.class);
    }

    private Client getClient(Settings settings) {
        Client client = TransportClient.builder().settings(settings).build();
        int port = settings.getAsInt("transport.port", 9300);
        String ip = settings.get("network.host", "localhost");
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, port);
        ((TransportClient) client).addTransportAddress(new InetSocketTransportAddress(inetSocketAddress));
        return client;
    }

    @Override
    public String name() {
        return "index-tools";
    }

    @Override
    public String description() {
        return "Manage elasticsearch index";
    }

    public static IndexToolsPlugin getStaticIndexToolsPlugin() {
        return indexToolsPlugin;
    }

    public static IndexToolsSettings getIndexToolsSettings() {
        return indexToolsPlugin.indexToolsSettings;
    }

    public static Client getEsClient() {
        return indexToolsPlugin.esClient;
    }
}
