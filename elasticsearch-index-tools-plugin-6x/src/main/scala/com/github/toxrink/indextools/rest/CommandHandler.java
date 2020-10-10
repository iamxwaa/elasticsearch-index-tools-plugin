package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.IndexToolsPlugin;
import com.github.toxrink.indextools.core.config.AutoCreateIndex;
import com.github.toxrink.indextools.core.config.IndexToolsSettings;
import com.github.toxrink.indextools.core.response.JsonResponse;
import com.github.toxrink.indextools.core.response.UnknownResponse;
import com.github.toxrink.indextools.core.security.GrantRun;
import com.github.toxrink.indextools.security.RestFilterPermission;
import com.github.toxrink.indextools.tools.AdminTools;
import org.elasticsearch.Version;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xw on 2019/9/19.
 */
public class CommandHandler extends AbstractRestHandler {
    private final IndexToolsPlugin plugin;

    public CommandHandler(Settings settings, RestController restController, IndexToolsPlugin plugin) {
        super(settings, restController);
        this.plugin = plugin;

        registerHandler("GET", "/_itools/info", "链接信息");
        registerHandler("GET", "/_itools/version", "插件版本信息");
        registerHandler("GET", "/_itools/index/precreate", "预创建索引");
        registerHandler("GET", "/_itools/index/hiscreate", "创建历史索引");
        registerHandler("PUT", "/_itools/index/aliascreate", "创建索引别名");
        registerHandler("GET", "/_itools/template", "模板配置列表");
        registerHandler("GET", "/_itools/template/load", "加载模板,参数:force=true/false");
        registerHandler("GET", "/_itools/right/load", "当前节点加载权限配置");
        registerHandler("GET", "/_itools/right/loadcluster", "全部节点加载权限配置");
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        IndexToolsSettings indexToolsSettings = plugin.getIndexToolsSettings();
        switch (request.path()) {
            case "/_itools/info":
                return channel -> channel.sendResponse(buildRestOkResponse(makeString(handlerList)));
            case "/_itools/version":
                Map<String, Object> data = new HashMap<>(2);
                data.put("version", Version.CURRENT.major);
                data.put("security", plugin.getIndexToolsSettings().securityConfig().basicEnable());
                data.put("autoCreate", plugin.getIndexToolsSettings().autoCreate().open());
                return channel -> channel.sendResponse(new JsonResponse(data));
            case "/_itools/index/precreate":
                AdminTools.preCreateIndex(indexToolsSettings.autoCreate().autoCreateIndices(), plugin.getEsClient());
                return channel -> channel.sendResponse(buildRestOkResponse("true"));
            case "/_itools/index/hiscreate":
                AdminTools.createHistoryIndex(indexToolsSettings.autoCreate().autoCreateIndices(), plugin.getEsClient());
                return channel -> channel.sendResponse(buildRestOkResponse("true"));
            case "/_itools/template":
                return channel -> channel.sendResponse(buildRestOkResponse(makeString(indexToolsSettings.templates())));
            case "/_itools/template/load":
                AdminTools.loadTemplates(indexToolsSettings.templates(), request.paramAsBoolean("force", false), plugin.getEsClient());
                return channel -> channel.sendResponse(buildRestOkResponse("true"));
            case "/_itools/right/load":
                RestFilterPermission.load(client);
                return channel -> channel.sendResponse(buildRestOkResponse("true"));
            case "/_itools/right/loadcluster":
                RestFilterPermission.loadCluster(client, indexToolsSettings, plugin.getEsClusterService().state().nodes());
                return channel -> channel.sendResponse(buildRestOkResponse("true"));
            case "/_itools/index/aliascreate":
                Map<String, String> param = GrantRun.parseJSON(request.content().utf8ToString(), LinkedHashMap.class);
                String indexName = param.get("indexName").trim();
                String indexNameFormat = param.get("indexNameFormat").trim();
                String indexExist = param.get("indexExist").trim();
                String indexFormat = param.get("indexFormat").trim();
                String aliasFormat = param.get("aliasFormat").trim();
                String timeField = param.get("timeField").trim();
                String[] startEndDate = param.get("startEndDate").split("-");
                String startDate = startEndDate[0].trim();
                String endDate = startEndDate.length == 1 ? startDate : startEndDate[1].trim();
                AutoCreateIndex autoCreateIndex = new AutoCreateIndex(indexName, indexNameFormat, indexFormat, aliasFormat, timeField, startDate, endDate);
                AdminTools.createAlias(autoCreateIndex, "0".equals(indexExist), !"0".equals(indexExist), plugin.getEsClient());
                return channel -> channel.sendResponse(buildRestOkResponse("true"));
        }
        return channel -> channel.sendResponse(new UnknownResponse());
    }

    private String makeString(List<String> list) {
        return list.stream().map(l -> l + "\n").collect(Collectors.joining());
    }

    private String makeString(scala.collection.immutable.List<String> list) {
        return list.mkString("\n");
    }
}
