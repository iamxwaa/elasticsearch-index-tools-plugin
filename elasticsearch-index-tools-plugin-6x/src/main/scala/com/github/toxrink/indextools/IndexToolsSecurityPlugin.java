package com.github.toxrink.indextools;

import com.github.toxrink.indextools.core.constants.IndexToolsConstants;
import com.github.toxrink.indextools.core.model.CheckResult;
import com.github.toxrink.indextools.core.security.RestFilter;
import com.github.toxrink.indextools.core.security.SecurityAction;
import com.github.toxrink.indextools.rest.PrivilegeHandler;
import com.github.toxrink.indextools.security.RestFilterPermission;
import com.github.toxrink.indextools.ssl.Netty4HttpsNettyTransport;
import com.github.toxrink.indextools.tools.AdminTools;
import org.apache.commons.logging.Log;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.plugins.NetworkPlugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import x.os.CmdWrapper;
import x.utils.JxUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Created by xw on 2019/10/14.
 */
public class IndexToolsSecurityPlugin extends IndexToolsPlugin implements NetworkPlugin {

    private static final Log LOG = JxUtils.getLogger(IndexToolsSecurityPlugin.class);

    private RestFilter restFilter;

    private SecurityAction userAction;

    private AtomicBoolean loadingPermission = new AtomicBoolean(true);

    public IndexToolsSecurityPlugin(Settings settings) {
        super(settings);
    }

    @Override
    public void onNodeStarted() {
        esThreadPool.schedule(TimeValue.timeValueSeconds(10), ThreadPool.Names.WARMER, () -> {
            try {
                if (indexToolsSettings.securityConfig().basicEnable()) {
                    //当索引初始化完毕后加载权限配置
                    int i = 60;
                    while (i > 0) {
                        List<ShardRouting> list = null;
                        try {
                            list = esClusterService.state().getRoutingTable().allShards(IndexToolsConstants.USER_PERMISSION_INDEX_NAME);
                        } catch (IndexNotFoundException e) {
                            AdminTools.checkAndCreateIndex(IndexToolsConstants.USER_PERMISSION_INDEX_NAME, esClient);
                            break;
                        }
                        boolean indexReady = true;
                        for (ShardRouting shardRouting : list) {
                            if (shardRouting.primary() && !shardRouting.active()) {
                                indexReady = false;
                                break;
                            }
                        }
                        if (indexReady) {
                            RestFilterPermission.load(esClient);
                            break;
                        }
                        i--;
                        CmdWrapper.sleep(5000);
                    }
                }
                AdminTools.loadTemplates(indexToolsSettings.templates(), false, esClient);
                if (indexToolsSettings.autoCreate().open()) {
                    AdminTools.preCreateIndex(indexToolsSettings.autoCreate().autoCreateIndices(), esClient);
                }
            } finally {
                loadingPermission.set(false);
            }
            loadScheduler();
            if (null != esScheduler) {
                LOG.info("Start index auto create scheduler");
                esScheduler.start();
            }
        });
    }

    @Override
    public UnaryOperator<RestHandler> getRestHandlerWrapper(ThreadContext threadContext) {
        if (indexToolsSettings.securityConfig().basicEnable()) {
            return rh -> restFilter.wrap(rh);
        }
        return null;
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings,
                                             RestController restController,
                                             ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings,
                                             SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        List<RestHandler> list = super.getRestHandlers(settings, restController, clusterSettings, indexScopedSettings, settingsFilter, indexNameExpressionResolver, nodesInCluster);
        list.add(new PrivilegeHandler(settings, restController, this));
        return list;
    }

    @Override
    public List<ActionFilter> getActionFilters() {
        if (indexToolsSettings.securityConfig().basicEnable()) {
            List<ActionFilter> list = new ArrayList<>(1);
            list.add(new VActionFilter());
            return list;
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<Object> createComponents(Client client,
                                               ClusterService clusterService,
                                               ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService,
                                               ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry,
                                               Environment environment,
                                               NodeEnvironment nodeEnvironment,
                                               NamedWriteableRegistry namedWriteableRegistry) {
        Collection<Object> collections = super.createComponents(client, clusterService, threadPool, resourceWatcherService, scriptService, xContentRegistry, environment, nodeEnvironment, namedWriteableRegistry);
        userAction = new SecurityAction(esThreadContext, indexToolsSettings, (name, password) ->
                RestFilterPermission.checkUserPermission(esClient, name, password)
        );
        restFilter = new RestFilter(userAction);
        return collections;
    }

    @Override
    public Map<String, Supplier<HttpServerTransport>> getHttpTransports(Settings settings,
                                                                        ThreadPool threadPool,
                                                                        BigArrays bigArrays,
                                                                        CircuitBreakerService circuitBreakerService,
                                                                        NamedWriteableRegistry namedWriteableRegistry,
                                                                        NamedXContentRegistry xContentRegistry,
                                                                        NetworkService networkService,
                                                                        HttpServerTransport.Dispatcher dispatcher) {
        if (indexToolsSettings.securityConfig().httpSSLEnable()) {
            LOG.info("Add https server");
            return Collections.singletonMap("idnex-tools-netty4-https", () -> new Netty4HttpsNettyTransport(settings, networkService, bigArrays, threadPool, xContentRegistry, dispatcher, this));
        }
        return Collections.emptyMap();
    }

    @Override
    public Settings additionalSettings() {
        final Settings.Builder builder = Settings.builder();
        if (esSettings.getAsBoolean(IndexToolsConstants.ITOOLS_SECURITY_HTTP_SSLONLY, false)) {
            builder.put(NetworkModule.HTTP_TYPE_KEY, "idnex-tools-netty4-https");
        }
        return builder.build();
    }

    public SecurityAction getSecurityAction() {
        return userAction;
    }


    public class VActionFilter implements ActionFilter {

        @Override
        public int order() {
            return 0;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse> void apply(Task task, String action, Request request, ActionListener<Response> listener, ActionFilterChain<Request, Response> chain) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("task ====> " + task);
                LOG.debug("action ====> " + action);
                LOG.debug("request ====> " + request);
                LOG.debug("CurrentThread ====> " + Thread.currentThread().getId());
            }
            if (loadingPermission.get()) {
                chain.proceed(task, action, request, listener);
                return;
            }
            CheckResult checkResult = RestFilterPermission.checkPermission(userAction, action, request);
            if (checkResult.status()) {
                chain.proceed(task, action, request, listener);
            } else {
                listener.onFailure(new IllegalAccessException(checkResult.reason()));
            }
        }
    }
}
