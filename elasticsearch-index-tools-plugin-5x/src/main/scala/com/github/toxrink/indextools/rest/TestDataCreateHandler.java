package com.github.toxrink.indextools.rest;

import com.github.toxrink.indextools.IndexToolsPlugin;
import com.github.toxrink.indextools.core.constants.IndexToolsConstants;
import com.github.toxrink.indextools.core.response.RestOkResponse;
import com.github.toxrink.indextools.core.response.UnknownResponse;
import com.github.toxrink.indextools.core.security.GrantRun;
import org.apache.commons.logging.Log;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import x.utils.JxUtils;
import x.utils.TimeUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.toxrink.indextools.core.constants.IndexToolsConstants.INDEX_GENERATETEST_POOL;


/**
 * Created by cz on 2019/12/16.
 */
public class TestDataCreateHandler extends AbstractRestHandler {

    private static Log logger = JxUtils.getLogger(TestDataCreateHandler.class);

    private static final int BATCH = 5000;

    private final IndexToolsPlugin plugin;

    private static Pattern regex = Pattern.compile("\\$\\{([^}]*)\\}");
    private static int initialized = 0;

    public TestDataCreateHandler(Settings settings, RestController restController, IndexToolsPlugin plugin) {
        super(settings,restController);
        this.plugin = plugin;
        //add test data pool
        registerHandler("GET", "/_itools/generatetest/pool");
        registerHandler("PUT", "/_itools/generatetest/pool");

        //init
        registerHandler("POST", "/_itools/generatetest/init");

        //create test data
        registerHandler("POST", "/_itools/generatetest/generate");

        registerHandler("GET", "/_itools/generatetest/generate/switch");
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
            if (!indicesAdminClient.prepareExists(INDEX_GENERATETEST_POOL).get().isExists()) {
                indicesAdminClient.prepareCreate(INDEX_GENERATETEST_POOL).execute().actionGet();
            }
            initialized = 1;
        }

        switch (request.path()) {
            case "/_itools/generatetest/init":
                //首次使用时初始化索引配置

            case "/_itools/generatetest/generate":
                //生成测试数据
                return generateTestData(request, client);
            case "/_itools/generatetest/generate/switch":
                //生成测试数据开关状态
                return channel -> channel.sendResponse(buildRestOkResponse(plugin.getIndexToolsSettings().settings().get(IndexToolsConstants.ITOOLS_AUTO_CREATE_TESTDATA, "false")));
            case "/_itools/generatetest/pool":
                switch (request.method()) {
                    case PUT:
                        String content = request.content().utf8ToString();
                        Map<String, Object> pool = GrantRun.parseJSON(content, LinkedHashMap.class);
                        BulkRequestBuilder bulkRequest = client.prepareBulk();
                        pool.forEach((k, v) -> {
                            Map<String, Object> json = getFieldPoolMap(k, v);
                            String type = "test";
                            bulkRequest.add(client.prepareIndex(INDEX_GENERATETEST_POOL, type, k).setSource(json));
                        });
                        BulkResponse bulkItemResponses = bulkRequest.execute().actionGet();
                        logger.debug(bulkItemResponses);
                        return channel -> channel.sendResponse(buildRestOkResponse("true"));
                    case GET:
                        break;
                }

        }
        return channel -> channel.sendResponse(new UnknownResponse());
    }

    private RestChannelConsumer generateTestData(RestRequest request, NodeClient client) {
        RestOkResponse aTrue;
        try {
            Map<String, Object> param = GrantRun.parseJSON(request.content().utf8ToString(), LinkedHashMap.class);
            List<Map<String, Object>> settings = (List<Map<String, Object>>) param.get("settings");
            for (Map<String, Object> setting : settings) {
                generateData(setting, client);
            }
            return channel -> channel.sendResponse(buildRestOkResponse("true"));
        } catch (Exception e) {
            logger.error(e);
            aTrue = buildRestOkResponse("false");
        }
        final RestOkResponse finalATrue = aTrue;
        return channel -> channel.sendResponse(finalATrue);
    }

    public static boolean generateData(Map<String, Object> setting, Client client) {
        logger.debug("setting:" + setting);
        String index = (String) (setting.get("index"));
        //校验索引
        ActionFuture<IndicesExistsResponse> exists = client.admin().indices().exists(new IndicesExistsRequest(index));
        if (!exists.actionGet().isExists()) {
            logger.error(index + "索引不存在!");
            return true;
        }

        //生成条数
        int rows = Integer.parseInt((String) setting.getOrDefault("rows", 100));
        String date = (String) setting.get("date");

        //获取内置数据池
        Map<String, Object> dataPool = getDataPool(client);
        //根据数据模板构造数据
        Map<String, Object> template = null;
        try {
            template = (Map<String, Object>) ((Map<String, Object>) setting.get("template")).get("data_json");
        } catch (Exception e) {
            logger.error(e);
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int flag = 0;
        String ltype = "logs";
        for (int i = 1; i < rows + 1; i++) {
            flag++;
            bulkRequest.add(client.prepareIndex(index, ltype).setSource(getStringJsonMap(rows, date, dataPool, template)));

            if (i % BATCH == 0) {
                logger.info("indexName:" + index + " total:" + rows + " already sended:" + i);
                bulkRequest.execute().actionGet();
                flag = 0;
                bulkRequest = client.prepareBulk();
            }
        }

        if (flag != 0) {
            bulkRequest.execute().actionGet();
            logger.info("indexName:" + index + " total:" + rows + " already sended:" + rows);
        }
        return false;
    }

    private Map<String, Object> getFieldPoolMap(String k, Object v) {
        Map<String, Object> json = new HashMap<>(2);
        json.put("field_name", k);
        json.put("values", v);
        return json;
    }

    private static Map<String, Object> getStringJsonMap(int rows, String date, Map<String, Object> dataPool, Map<String, Object> template) {
        //获取时间范围
        Date[] range = getDateRange(date);
        Map<String, Object> jsonData = new HashMap<>(rows);
        Date finalStart = range[0];
        Date finalEnd = range[1];
        template.entrySet().forEach(r -> {
            String el;
            Object value = r.getValue();
            if (value instanceof String) {
                if ((el = el(value.toString())) != null) {
                    //${} 表达式, 先判断是时间格式, 否则从数据池里取值
                    if (el.startsWith("yyyy-MM") || el.startsWith("yyyy.MM")) {
                        //小于10的都认为是时间字符串
                        if (el.length() <= 10) {
                            //yyyy-MM-dd
                            value = TimeUtils.format(getRandomTime(finalStart, finalEnd), el);
                        } else {
                            //yyyy-MM-dd HH:mm:ss
                            value = getRandomTime(finalStart, finalEnd);
                        }
                    } else {
                        value = dataPool.get(el);
                    }
                }
            }
            if (value instanceof Object[]) {
                Object[] values = (Object[]) value;
                jsonData.put(r.getKey(), values[(int) (Math.random() * values.length)]);
            } else if (value instanceof List) {
                List values = (List) value;
                jsonData.put(r.getKey(), values.get((int) (Math.random() * values.size())));
            } else {
                jsonData.put(r.getKey(), value);
            }
        });
        return jsonData;
    }

    private static Date[] getDateRange(String date) {
        Date timeRange = null;
        Date[] range = new Date[2];
        if (date.length() == 7) {
            //某月
            timeRange = TimeUtils.parese(date, "yyyy-MM");
            range[0] = TimeUtils.getFirstDayOfMonth(timeRange);
            range[1] = TimeUtils.getLastDayOfMonth(timeRange);
        } else {
            //某天
            timeRange = TimeUtils.parese(date, "yyyy-MM-dd");
            range[1] = timeRange;
            range[0] = timeRange;
        }
        return range;
    }

    private static Map<String, Object> getDataPool(Client client) {
        SearchResponse dataPoolResponse = client.prepareSearch(INDEX_GENERATETEST_POOL).setSize(1000).execute().actionGet();
        Long totalDataHits = dataPoolResponse.getHits().getTotalHits();
        Map<String, Object> res = new HashMap<>(totalDataHits.intValue());
        dataPoolResponse.getHits().iterator().forEachRemaining(hit -> {
            Object values = hit.getSourceAsMap().get("values");
            if (values != null) {
                res.put(hit.getId(), values);
            }
        });
        return res;
    }
 

    public static String el(String arg) {
        Matcher matcher = regex.matcher(arg);
        while (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 按范围(不要跨月)获取随机时间
     *
     * @return
     */
    public static Date getRandomTime(Date startDate, Date endDate) {
        if (startDate.getMonth() != endDate.getMonth()) {
            return new Date();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);

        if (!startDate.equals(endDate)) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(startDate);
            end.setTime(endDate);

            int startDay = start.get(Calendar.DAY_OF_YEAR);
            int margin = end.get(Calendar.DAY_OF_YEAR) - startDay;

            cal.set(Calendar.DAY_OF_YEAR, startDay + new Random().nextInt(margin));
        }

        cal.set(Calendar.HOUR, new Random().nextInt(23));
        cal.set(Calendar.MINUTE, new Random().nextInt(59));
        cal.set(Calendar.SECOND, new Random().nextInt(59));
        return cal.getTime();
    }

}
