package com.github.toxrink.indextools.rest;


import com.github.toxrink.indextools.core.constants.IndexToolsConstants;
import com.github.toxrink.indextools.core.response.RestOkResponse;
import com.github.toxrink.indextools.core.response.UnknownResponse;
import org.apache.commons.logging.Log;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import x.utils.JxUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Created by cz on 2019/12/16.
 */
public class TestDataTemplateHandler extends AbstractRestHandler {

    private int initialized = 0;
    private final RestController restController;

    private static Log logger = JxUtils.getLogger(TestDataTemplateHandler.class);
//    private static final int BATCH = 5000;

    public static final String TYPE = "test";

    public TestDataTemplateHandler(Settings settings, RestController restController) {
        super(settings,restController);
        this.restController = restController;
        // template
        registerHandler("POST", "/_itools/generatetest/template");
        registerHandler("PUT", "/_itools/generatetest/template/{id}");
        registerHandler("GET", "/_itools/generatetest/template/{id}");
        registerHandler("DELETE", "/_itools/generatetest/template/{id}");
    }


    /**
     * Prepare the request for execution. Implementations should consume all request params before
     * returning the runnable for actual execution. Unconsumed params will immediately terminate
     * execution of the request. However, some params are only used in processing the response;
     * implementations can override {@link BaseRestHandler#responseParams()} to indicate such
     * params.
     *
     * @param request the request to execute
     * @param client  client for executing actions on the local node
     * @return the action to execute
     * @throws IOException if an I/O exception occurred parsing the request and preparing for
     *                     execution
     */
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {

        if (initialized == 0) {
            IndicesAdminClient indicesAdminClient = client.admin().indices();
            if (!indicesAdminClient.prepareExists(IndexToolsConstants.INDEX_GENERATETEST_TEMPLATE).get().isExists()) {
                indicesAdminClient.prepareCreate(IndexToolsConstants.INDEX_GENERATETEST_TEMPLATE).execute().actionGet();
                indicesAdminClient.prepareCreate(IndexToolsConstants.INDEX_GENERATETEST_POOL).execute().actionGet();
            }
            initialized = 1;
        }

        switch (request.method()) {
            case POST:
                return query(client);
            case PUT:
                return create(request, client);
            case DELETE:
                return delete(request, client);
        }
        return channel -> channel.sendResponse(new UnknownResponse());
    }

    private RestChannelConsumer delete(RestRequest request, NodeClient client) {
        String id = request.param("id","0");
        DeleteResponse deleteResponse = client.prepareDelete(IndexToolsConstants.INDEX_GENERATETEST_TEMPLATE, TYPE, id).execute().actionGet();
        logger.warn("删除数据模板:"+ deleteResponse);
        if(deleteResponse.status()== RestStatus.OK){
            return channel -> channel.sendResponse( buildRestOkResponse("true"));
        }
        return channel -> channel.sendResponse(new UnknownResponse());
    }

    private RestChannelConsumer create(RestRequest request, NodeClient client) {
        String content = request.content().utf8ToString();
        String id = request.param("id","0");
        DeleteResponse test = client.prepareDelete(IndexToolsConstants.INDEX_GENERATETEST_TEMPLATE, TYPE, id).execute().actionGet();
        if(test.status()== RestStatus.OK || test.status()== RestStatus.NOT_FOUND){
            IndexResponse indexResponse = client.prepareIndex(IndexToolsConstants.INDEX_GENERATETEST_TEMPLATE, TYPE, id).setSource(content, XContentType.JSON).setCreate(true).execute().actionGet();
            logger.warn("更新数据模板:"+ indexResponse);
        }
//              UpdateResponse updateResponse = client.prepareUpdate("generatetest-template", "test", id)
//                        .setDoc(content, XContentType.JSON).execute().actionGet();
//        logger.info(content);
        return channel -> channel.sendResponse( buildRestOkResponse("true"));
    }

    private RestChannelConsumer query(NodeClient client) {
        SearchResponse searchResponse = client.prepareSearch(IndexToolsConstants.INDEX_GENERATETEST_TEMPLATE).setSize(1000).execute().actionGet();
        Map<String, Object> data = new LinkedHashMap<>();
        Long totalHits = searchResponse.getHits().getTotalHits().value;
        data.put("count", totalHits);
        Map<String, Object> res = new HashMap<>(totalHits.intValue());
        searchResponse.getHits().iterator().forEachRemaining(hit -> {
            res.put(hit.getId(), hit.getSourceAsMap());
        });
        data.put("data", res);
        data.put("code", 0);
        data.put("msg", searchResponse.status().name());
        return ch -> ch.sendResponse(new RestOkResponse(data));
    }
 
 
}
