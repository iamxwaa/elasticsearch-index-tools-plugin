package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.core.response.RestOkResponse;
import org.apache.commons.logging.Log;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import x.utils.JxUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xw on 2019/12/25.
 */
public abstract class AbstractRestHandler extends BaseRestHandler {

    private static final Log LOG = JxUtils.getLogger(AbstractRestHandler.class);

    private final RestController restController;

    protected final List<String> handlerList = new ArrayList<>();

    protected AbstractRestHandler(Settings settings, RestController restController, Client client) {
        super(settings, restController, client);
        this.restController = restController;
    }

    /**
     * 注册请求
     *
     * @param method 请求方法
     * @param path   请求路径
     */
    protected void registerHandler(String method, String path) {
        LOG.info("add handler " + method + "#" + path);
        restController.registerHandler(RestRequest.Method.valueOf(method), path, this);
    }

    /**
     * 注册请求
     *
     * @param method  请求方法
     * @param path    请求路径
     * @param comment 功能注释
     */
    protected void registerHandler(String method, String path, String comment) {
        handlerList.add(method + "\t" + path + "\t" + comment);
        restController.registerHandler(RestRequest.Method.valueOf(method), path, this);
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * es会对请求参数做校验,所以要重新获取参数
     *
     * @param request
     * @return
     */
    protected Map<String, String> skipParamCheck(RestRequest request) {
        Map<String, String> params = new HashMap<>(request.params().size());
        request.params().forEach((k, v) -> {
            request.param(k);
            params.put(k, v);
        });
        return params;
    }

    /**
     * 构造成功返回对象
     *
     * @param msg
     * @return
     */
    protected RestOkResponse buildRestOkResponse(String msg) {
        return new RestOkResponse(msg);
    }

    /**
     * 构造成功返回对象
     *
     * @param data
     * @return
     */
    protected RestOkResponse buildRestOkResponse(Map<String, Object> data) {
        return new RestOkResponse(data);
    }
}
