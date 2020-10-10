package com.github.toxrink.indextools.rest;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.github.toxrink.indextools.core.config.AutoCreateIndex;
import com.github.toxrink.indextools.core.constants.IndexToolsConstants;
import com.github.toxrink.indextools.core.response.JsonResponse;
import com.github.toxrink.indextools.core.response.RestOkResponse;
import com.github.toxrink.indextools.core.response.UnknownResponse;
import com.github.toxrink.indextools.core.security.GrantRun;
import com.github.toxrink.indextools.security.RestFilterPermission;
import com.github.toxrink.indextools.tools.AdminTools;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.elasticsearch.Version;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.*;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTimeZone;
import x.utils.JxUtils;
import x.utils.TimeUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by lil on 2020/03/04
 */
public class StatisticDataHandler extends AbstractRestHandler {

    private int initialized = 0;
    private final RestController restController;

    private static Log log = JxUtils.getLogger(StatisticDataHandler.class);

    public StatisticDataHandler(Settings settings, RestController restController) {
        super(settings,restController);
        this.restController = restController;

        registerHandler("POST", "/_itools/statistic");
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

        switch (request.path()) {
            case "/_itools/statistic":

                log.info("param ===> "+request.content().utf8ToString());
                Map<String, Object> param = GrantRun.parseJSON(request.content().utf8ToString(), LinkedHashMap.class);
                String indexName = (String) param.get("indexNames");
                String[] indexNames = indexName.split(",");
                String start_time = (String) param.get("start_time");
                String end_time = (String) param.get("end_time");
//                String mustData = (String) param.get("mustData");
                String statisticType = (String) param.get("statisticType");
                String fieldData = (String) param.get("fieldData");

                List<Map<String,String>> mustList = (List<Map<String, String>>) param.get("mustData");
//                List<Map<String,String>> mustList = new Gson().fromJson(mustData, new TypeToken<List<LinkedHashMap<String,String>>>() {}.getType());
//                String subStatisticData = (String) param.get("subStatisticData");
                List<Map<String,String>> subStatisticList = (List<Map<String, String>>) param.get("subStatisticData");
//                List<Map<String,String>> subStatisticList = new Gson().fromJson(subStatisticData, new TypeToken<List<LinkedHashMap<String,String>>>() {}.getType());

                BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                if (StringUtils.isNotEmpty(start_time) && StringUtils.isNotEmpty(end_time)) {
                    queryBuilder.must(QueryBuilders.rangeQuery("event_time").from(gmtToUtcTime(TimeUtils.parese(start_time, "yyyy-MM-dd HH:mm:ss")))
                            .to(gmtToUtcTime(TimeUtils.parese(end_time, "yyyy-MM-dd HH:mm:ss"))));
                }
                for (Map<String,String> must : mustList) {
                    String mustType = must.get("mustType");
                    String mustField = must.get("mustField");
                    String termType = must.get("termType");
                    if (StringUtils.isEmpty(mustField)){
                        continue;
                    }
                    if ("must".equals(mustType)){
                        if ("term".equals(termType)){
                            queryBuilder.must(QueryBuilders.termQuery(mustField, must.get("mustFieldData")));
                        }else{
                            RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(mustField);
                            handlerRangeQuery(rangeQueryBuilder, must);
                            queryBuilder.must(rangeQueryBuilder);
                        }
                    }else if ("must_not".equals(mustType)){
                        if ("term".equals(termType)){
                            queryBuilder.mustNot(QueryBuilders.termQuery(mustField, must.get("mustFieldData")));
                        }else{
                            RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(mustField);
                            handlerRangeQuery(rangeQueryBuilder, must);
                            queryBuilder.mustNot(rangeQueryBuilder);
                        }
                    }else if ("should".equals(mustType)){
                        if ("term".equals(termType)){
                            queryBuilder.should(QueryBuilders.termQuery(mustField, must.get("mustFieldData")));
                        }else{
                            RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(mustField);
                            handlerRangeQuery(rangeQueryBuilder, must);
                            queryBuilder.should(rangeQueryBuilder);
                        }
                    }
                }
                SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexNames).setTypes("logs")
                        .setQuery(queryBuilder).setSize(0);
                if ("dateHis".equals(statisticType)){
                    DateHistogramAggregationBuilder dateAgg = AggregationBuilders.dateHistogram("oneAgg");
                    dateAgg.field("event_time");
                    dateAgg.dateHistogramInterval(DateHistogramInterval.DAY);
                    dateAgg.timeZone(DateTimeZone.forOffsetHours(8));
                    dateAgg.format("yyyy-MM-dd HH:mm:ss");
                    handlerAgg(subStatisticList, null, dateAgg);

                    searchRequestBuilder.addAggregation(dateAgg);
                }else {
                    AbstractAggregationBuilder agg = null;
                    if ("terms".equals(statisticType)){
                        agg = AggregationBuilders.terms("oneAgg").field(fieldData);
                    }else if ("cardinality".equals(statisticType)){
                        agg = AggregationBuilders.cardinality("oneAgg").field(fieldData);
                    }else if ("sum".equals(statisticType)){
                        agg = AggregationBuilders.sum("oneAgg").field(fieldData);
                    }else {
                        agg = AggregationBuilders.count("oneAgg").field(fieldData);
                    }
                    handlerAgg(subStatisticList, agg, null);
                    searchRequestBuilder.addAggregation(agg);
                }
                log.info("***** searchRequestBuilder ******\n" + searchRequestBuilder.toString() + "***********\n");
                log.info("***** request ******\n" + searchRequestBuilder.request() + "***********\n");
                SearchResponse response = searchRequestBuilder.get();
                log.info("***** getAggregations ******\n" + response + "***********\n");

                Map<String,Object> map = new HashMap<>();
                handlerAggResponse(response.getAggregations().get("oneAgg"), map);

                Map<String,Object> result = new LinkedHashMap<>();
                result.put("aggData", map);
                result.put("aggResource", response.getAggregations());
                result.put("requestSource", searchRequestBuilder.toString());
                result.put("code", 0);
                result.put("msg", response.status().name());
                return channel -> channel.sendResponse(new RestOkResponse(result));
        }
        return channel -> channel.sendResponse(new UnknownResponse());
    }

    private void handlerAggResponse(Object oneAgg, Map<String,Object> result) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (oneAgg instanceof StringTerms) {
            StringTerms oneAggTerms = (StringTerms) oneAgg;
            for (StringTerms.Bucket temp : oneAggTerms.getBuckets()) {
                Map<String, Object> map = new HashMap<>();
                map.put(temp.getKeyAsString(), temp.getDocCount());
                for (Aggregation aggregation : temp.getAggregations().asList()) {
                    handlerAggResponse(aggregation, map);
                }
                resultList.add(map);
            }
            result.put("result", resultList);
        } else if (oneAgg instanceof InternalCardinality){
            InternalCardinality terms = (InternalCardinality) oneAgg;
            result.put("distinctCount", terms.getValue());
        } else if (oneAgg instanceof InternalSum){
            InternalSum temp = (InternalSum) oneAgg;
            result.put("sumTotal", temp.getValue());
        } else if (oneAgg instanceof InternalDateHistogram) {
            InternalDateHistogram histogram = (InternalDateHistogram) oneAgg;
            for (InternalDateHistogram.Bucket entry : histogram.getBuckets()) {
                Map<String, Object> map = new HashMap<>();
                map.put(entry.getKeyAsString(), entry.getDocCount());
                for (Aggregation aggregation : entry.getAggregations().asList()) {
                    handlerAggResponse(aggregation, map);
                }
                resultList.add(map);
            }
            result.put("result", resultList);
        } else if (oneAgg instanceof InternalValueCount){
            InternalValueCount temp = (InternalValueCount) oneAgg;
            result.put("countValue", temp.getValue());
        }
    }

    private void handlerAgg(List<Map<String, String>> subStatisticList, AbstractAggregationBuilder agg, DateHistogramAggregationBuilder dateAgg) {
        for (Map<String, String> map : subStatisticList) {
            String subStatisticType = map.get("subStatisticType");
            String subFieldData = map.get("subFieldData");
            AbstractAggregationBuilder subAgg = null;
            if ("terms".equals(subStatisticType)){
                subAgg = AggregationBuilders.terms(subFieldData+"TermAgg").field(subFieldData);
            }else if ("cardinality".equals(subStatisticType)){
                subAgg = AggregationBuilders.cardinality(subFieldData+"CAgg").field(subFieldData);
            }else if ("sum".equals(subStatisticType)){
                subAgg = AggregationBuilders.sum(subFieldData+"SumAgg").field(subFieldData);
            }else {
                subAgg = AggregationBuilders.count(subFieldData+"CountAgg").field(subFieldData);
            }
            if (agg != null){
                agg.subAggregation(subAgg);
            }else{
                dateAgg.subAggregation(subAgg);
            }
        }
    }

    private void handlerRangeQuery(RangeQueryBuilder rangeQueryBuilder, Map<String, String> must) {
        if ("gt".equals(must.get("gtType"))){
            rangeQueryBuilder.gt(must.get("gtData"));
        }else{
            rangeQueryBuilder.gte(must.get("gtData"));
        }
        if ("lt".equals(must.get("ltType"))){
            rangeQueryBuilder.lt(must.get("ltData"));
        }else{
            rangeQueryBuilder.lte(must.get("ltData"));
        }
    }

    private String gmtToUtcTime(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.HOUR, -8);
        String TIME_FMT_1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        String date = new SimpleDateFormat(TIME_FMT_1).format(cal.getTime());
        return date;
    }

}
