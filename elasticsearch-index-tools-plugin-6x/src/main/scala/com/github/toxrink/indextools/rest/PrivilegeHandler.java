package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.IndexToolsSecurityPlugin;
import com.github.toxrink.indextools.core.response.CheckResponse;
import com.github.toxrink.indextools.security.RestFilterPermission;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import x.utils.StrUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.toxrink.indextools.core.constants.IndexToolsConstants.TYPE;
import static com.github.toxrink.indextools.core.constants.IndexToolsConstants.USER_PERMISSION_INDEX_NAME;

/**
 * Created by xw on 2019/10/15.
 */
public class PrivilegeHandler extends AbstractRestHandler {
    private final IndexToolsSecurityPlugin plugin;

    public PrivilegeHandler(Settings settings, RestController restController, IndexToolsSecurityPlugin plugin) {
        super(settings, restController);
        this.plugin = plugin;
        registerHandler("GET", "/_itools/user");
        registerHandler("DELETE", "/_itools/user/{id}");
        registerHandler("PUT", "/_itools/user/{id}/{name}");
        registerHandler("POST", "/_itools/user");
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        RestResponse restResponse = null;
        switch (request.method()) {
            case GET:
                int from = request.paramAsInt("page", 1);
                int size = request.paramAsInt("limit", 10);
                SearchResponse searchResponse = client.prepareSearch(USER_PERMISSION_INDEX_NAME).setTypes(TYPE).setFrom((from - 1) * size).setSize(size).execute().actionGet();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("count", searchResponse.getHits().totalHits);
                ArrayList<Map<String, Object>> list = new ArrayList<>(size);
                searchResponse.getHits().iterator().forEachRemaining(hit -> {
                    Map<String, Object> tmp = hit.getSourceAsMap();
                    tmp.put("id", hit.getId());
                    list.add(tmp);
                });
                data.put("data", list);
                data.put("code", 0);
                data.put("msg", searchResponse.status().name());
                restResponse = buildRestOkResponse(data);
                break;
            case PUT:
                String content = request.content().utf8ToString();
                String id = request.param("id", "0");
                String name = request.param("name");
                String esID = getId(name);
                if ("0".equals(id)) {
                    if (plugin.getIndexToolsSettings().securityConfig().superUser().equals(name)) {
                        restResponse = new CheckResponse("用户名异常", RestStatus.CONFLICT);
                        break;
                    }
                    GetResponse getResponse = client.prepareGet(USER_PERMISSION_INDEX_NAME, TYPE, esID).execute().actionGet();
                    if (getResponse.isExists()) {
                        restResponse = new CheckResponse("用户名已存在", RestStatus.CONFLICT);
                        break;
                    }
                    IndexResponse indexResponse = client.prepareIndex(USER_PERMISSION_INDEX_NAME, TYPE).setId(esID).setSource(content, XContentType.JSON).execute().actionGet();
                    restResponse = buildRestOkResponse(indexResponse.status().name());
                } else {
                    if (!esID.equals(id)) {
                        restResponse = new CheckResponse("用户信息异常", RestStatus.CONFLICT);
                        break;
                    }
                    UpdateResponse updateResponse = client.prepareUpdate(USER_PERMISSION_INDEX_NAME, TYPE, id).setDoc(content, XContentType.JSON).execute().actionGet();
                    restResponse = buildRestOkResponse(updateResponse.status().name());
                }
                reloadPermission(client);
                break;
            case DELETE:
                DeleteResponse deleteResponse = client.prepareDelete(USER_PERMISSION_INDEX_NAME, TYPE, request.param("id")).execute().actionGet();
                restResponse = buildRestOkResponse(deleteResponse.status().name());
                reloadPermission(client);
                break;
        }

        final RestResponse frestResponse = restResponse;
        return ch -> ch.sendResponse(frestResponse);
    }

    private void reloadPermission(NodeClient client) {
        client.admin().indices().refresh(new RefreshRequest().indices(USER_PERMISSION_INDEX_NAME)).actionGet();
        RestFilterPermission.loadCluster(client, plugin.getIndexToolsSettings(), plugin.getEsClusterService().state().nodes());
    }

    private String getId(String username) {
        return StrUtils.toMD5("X-INDEX-TOOLS-X-" + username);
    }
}
