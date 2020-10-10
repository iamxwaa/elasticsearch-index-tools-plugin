package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.core.response.CssResponse;
import com.github.toxrink.indextools.core.response.JavaScriptResponse;
import com.github.toxrink.indextools.core.response.OtherResponse;
import com.github.toxrink.indextools.core.response.PageResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;

/**
 * Created by xw on 2019/9/4.
 */
public class ResourceHandler extends AbstractRestHandler {
    public ResourceHandler(Settings settings, RestController restController) {
        super(settings, restController);
        registerHandler("GET", "/_itools");
        registerHandler("GET", "/_itools/resources/*");
        registerHandler("GET", "/_itools/resources/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*/*/*/*");
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        skipParamCheck(request);
        if (request.path().equals("/_itools")) {
            return channel -> channel.sendResponse(new PageResponse(request));
        } else if (request.path().endsWith(".html")) {
            return channel -> channel.sendResponse(new PageResponse(request));
        } else if (request.path().endsWith(".js")) {
            return channel -> channel.sendResponse(new JavaScriptResponse(request));
        } else if (request.path().endsWith(".css")) {
            return channel -> channel.sendResponse(new CssResponse(request));
        } else {
            return channel -> channel.sendResponse(new OtherResponse(request));
        }
    }
}
