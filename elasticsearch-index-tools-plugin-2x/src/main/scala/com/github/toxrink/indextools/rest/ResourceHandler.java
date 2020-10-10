package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.core.response.OtherResponse;
import com.github.toxrink.indextools.response.CssResponse;
import com.github.toxrink.indextools.response.JavaScriptResponse;
import com.github.toxrink.indextools.response.PageResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

/**
 * Created by xw on 2019/9/4.
 */
public class ResourceHandler extends AbstractRestHandler {

    @Inject
    public ResourceHandler(Settings settings, Client client, RestController restController) {
        super(settings, restController, client);
        registerHandler("GET", "/_itools");
        registerHandler("GET", "/_itools/resources/*");
        registerHandler("GET", "/_itools/resources/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*/*/*");
        registerHandler("GET", "/_itools/resources/*/*/*/*/*/*");
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        skipParamCheck(request);
        if (request.path().equals("/_itools")) {
            channel.sendResponse(new PageResponse(request));
        } else if (request.path().endsWith(".html")) {
            channel.sendResponse(new PageResponse(request));
        } else if (request.path().endsWith(".js")) {
            channel.sendResponse(new JavaScriptResponse(request));
        } else if (request.path().endsWith(".css")) {
            channel.sendResponse(new CssResponse(request));
        } else {
            channel.sendResponse(new OtherResponse(request));
        }
    }
}
