package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.core.response.UnknownResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.toxrink.indextools.core.constants.IndexToolsConstants.AUTO_CREATE_INDEX_NAME;
import static com.github.toxrink.indextools.core.constants.IndexToolsConstants.TYPE;

/**
 * Created by xw on 2019/10/15.
 */
public class AutoCreateHandler extends AbstractRestHandler {

    public AutoCreateHandler(Settings settings, RestController restController) {
        super(settings, restController);
        registerHandler("GET", "/_itools/autocreate");
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        switch (request.method()) {
            case GET:
                int from = request.paramAsInt("page", 1);
                int size = request.paramAsInt("limit", 10);
                SearchResponse searchResponse = client.prepareSearch(AUTO_CREATE_INDEX_NAME).setTypes(TYPE).setFrom((from - 1) * size).setSize(size).execute().actionGet();
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
                return ch -> ch.sendResponse(buildRestOkResponse(data));
        }
        return ch -> ch.sendResponse(new UnknownResponse());
    }
}
