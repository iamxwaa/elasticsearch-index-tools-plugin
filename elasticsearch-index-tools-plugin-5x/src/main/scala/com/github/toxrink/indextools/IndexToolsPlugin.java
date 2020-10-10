package com.github.toxrink.indextools;

import com.github.toxrink.indextools.config.EsSettings5x;
import com.github.toxrink.indextools.core.config.EsSettings;
import com.github.toxrink.indextools.core.config.IndexToolsSettings;
import com.github.toxrink.indextools.core.constants.IndexToolsConstants;
import com.github.toxrink.indextools.core.quartz.EsScheduler;
import com.github.toxrink.indextools.core.security.GrantRun;
import com.github.toxrink.indextools.quartz.DefaultJobImpl;
import com.github.toxrink.indextools.quartz.TestDataJobImpl;
import com.github.toxrink.indextools.rest.*;
import com.github.toxrink.indextools.tools.AdminTools;
import org.apache.commons.logging.Log;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.ClusterPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.quartz.JobDataMap;
import x.utils.JxUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by xw on 2019/9/18.
 */
public class IndexToolsPlugin extends Plugin implements ClusterPlugin, ActionPlugin {

    private static final Log LOG = JxUtils.getLogger(IndexToolsPlugin.class);

    protected EsSettings esSettings;

    protected IndexToolsSettings indexToolsSettings;
    protected EsScheduler esScheduler;

    protected Client esClient;
    protected ThreadPool esThreadPool;
    protected ThreadContext esThreadContext;
    protected ClusterService esClusterService;

    public IndexToolsPlugin(Settings settings) {
        esSettings = new EsSettings5x(settings);
        indexToolsSettings = new IndexToolsSettings(esSettings);
    }

    public void onNodeStarted() {
        esThreadPool.schedule(TimeValue.timeValueSeconds(20), ThreadPool.Names.WARMER, () -> {
            AdminTools.loadTemplates(indexToolsSettings.templates(), false, esClient);
            if (indexToolsSettings.autoCreate().open()) {
                AdminTools.preCreateIndex(indexToolsSettings.autoCreate().autoCreateIndices(), esClient);
            }
            loadScheduler();
            if (null != esScheduler) {
                LOG.info("Start index auto create scheduler");
                esScheduler.start();
            }
        });
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings,
                                             RestController restController,
                                             ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings,
                                             SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        List<RestHandler> list = new ArrayList<>();
        list.add(new CommandHandler(settings, restController, this));
        list.add(new ResourceHandler(settings, restController));
        list.add(new AutoCreateHandler(settings, restController));
        list.add(new TestDataCreateHandler(settings, restController,this));
        list.add(new TestDataTemplateHandler(settings, restController));
        list.add(new StatisticDataHandler(settings, restController));
        return list;
    }

    protected void loadScheduler() {
        if (null != esScheduler) {
            esScheduler.stop();
            esScheduler.clear();
            esScheduler = null;
        }

        esScheduler = GrantRun.applyJava(() -> {
            EsScheduler esScheduler2 = new EsScheduler();
            final Properties props = new Properties();
            props.put("org.quartz.scheduler.instanceName", "static-index-tools");
            props.put("org.quartz.threadPool.threadCount", "3");
            esScheduler2.init(props);
            //加载索引自动创建
            if (indexToolsSettings.autoCreate().open()) {
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put(IndexToolsConstants.INDEX_CREATE_JOB, this);

                esScheduler2.addSchedule("static-index-tools"
                        , indexToolsSettings.checkInterval()
                        , DefaultJobImpl.class, jobDataMap);
            }
            //加载自动创建测试数据
            if (esSettings.getAsBoolean(IndexToolsConstants.ITOOLS_AUTO_CREATE_TESTDATA, false)) {
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put(IndexToolsConstants.INDEX_CREATE_JOB, this);
                esScheduler2.addSchedule("static-test-index-tools"
                        , indexToolsSettings.testDataInterval()
                        , TestDataJobImpl.class, jobDataMap);
            }

            return esScheduler2;
        });
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry) {
        this.esClient = client;
        this.esThreadPool = threadPool;
        this.esThreadContext = threadPool.getThreadContext();
        this.esClusterService = clusterService;
        onNodeStarted();
        return Collections.emptyList();
    }

    @Override
    public void close() throws IOException {
        if (null != esScheduler) {
            esScheduler.stop();
            esScheduler.clear();
        }
        super.close();
    }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<Setting<?>>();
        settings.add(Setting.affixKeySetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_PREFIX,
                IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_NAME,
                (key) -> Setting.simpleString(key, Setting.Property.NodeScope, Setting.Property.Dynamic)));
        settings.add(Setting.affixKeySetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_PREFIX,
                IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_NAME_FORMAT,
                (key) -> Setting.simpleString(key, Setting.Property.NodeScope, Setting.Property.Dynamic)));
        settings.add(Setting.affixKeySetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_PREFIX,
                IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_FORMAT,
                (key) -> Setting.simpleString(key, Setting.Property.NodeScope, Setting.Property.Dynamic)));
        settings.add(Setting.affixKeySetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_PREFIX,
                IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_ALIAS_FORMAT,
                (key) -> Setting.simpleString(key, Setting.Property.NodeScope, Setting.Property.Dynamic)));
        settings.add(Setting.affixKeySetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_PREFIX,
                IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_TIME_FIELD,
                (key) -> Setting.simpleString(key, Setting.Property.NodeScope, Setting.Property.Dynamic)));
        settings.add(Setting.affixKeySetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_PREFIX,
                IndexToolsConstants.ITOOLS_AUTO_CREATE_INDEX_CREATE_HISTORY,
                (key) -> Setting.listSetting(key, Collections.emptyList(), Function.identity(), Setting.Property.NodeScope, Setting.Property.Dynamic)));
        settings.add(Setting.boolSetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_OPEN, false, Setting.Property.NodeScope));
        settings.add(Setting.listSetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_AVALIABLE_INDEX, Collections.emptyList(), Function.identity(), Setting.Property.NodeScope));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_AUTO_CHECK_TIME, Setting.Property.NodeScope));

        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_TEMPLATES, Setting.Property.NodeScope));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_RESOURCE, Setting.Property.NodeScope, Setting.Property.Filtered));

        settings.add(Setting.boolSetting(IndexToolsConstants.ITOOLS_SECURITY_BASIC, false, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_USERNAME, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_PASSWORD, Setting.Property.NodeScope, Setting.Property.Filtered));

        settings.add(Setting.boolSetting(IndexToolsConstants.ITOOLS_SECURITY_HTTP_SSLONLY, false, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_HTTP_KEYSTORE, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_HTTP_CERTIFICATE_PASSWORD, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_HTTP_KEYSTORE_PASSWORD, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_HTTP_ALGORITHM, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_SECURITY_HTTP_PROTOCOL, Setting.Property.NodeScope, Setting.Property.Filtered));

        settings.add(Setting.boolSetting(IndexToolsConstants.ITOOLS_AUTO_CREATE_TESTDATA, false, Setting.Property.NodeScope));
        settings.add(Setting.simpleString(IndexToolsConstants.ITOOLS_AUTO_CREATE_TESTDATA_CHECK_TIME, Setting.Property.NodeScope));
        return settings;
    }

    public ClusterService getEsClusterService() {
        return esClusterService;
    }

    public Client getEsClient() {
        return esClient;
    }

    public IndexToolsSettings getIndexToolsSettings() {
        return indexToolsSettings;
    }

    public ThreadContext getEsThreadContext() {
        return esThreadContext;
    }
}
