/*
 * Copyright (c) 2017-2017 Globo.com
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globocom.grou.report.ts.opentsdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globocom.grou.SystemEnv;
import com.globocom.grou.entities.Test;
import com.globocom.grou.report.ts.TSClient;
import com.google.common.collect.ImmutableMap;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class OpenTSDBClient implements TSClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTSDBClient.class);

    private static final AsyncHttpClient HTTP_CLIENT;
    static {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
                .setAllowPoolingConnections(true)
                .setFollowRedirect(false)
                .setCompressionEnforced(false)
                .setConnectTimeout(2000)
                .setMaxConnectionsPerHost(100)
                .setMaxConnections(100)
                .setAcceptAnyCertificate(true).build();
        HTTP_CLIENT = new AsyncHttpClient(config);
    }

    private static final String OPENTSDB_URL = ResourceEnv.OPENTSDB_URL.getValue();

    private static final String METRICS_PREFIX = SystemEnv.TS_METRIC_PREFIX.getValue();

    private final Map<String, Pair<String, Set<Filter>>> metricsMap = ImmutableMap.<String, Pair<String, Set<Filter>>>builder()
        .put(METRICS_PREFIX + ".loaders.cpu.mean",          Pair.of("loaders_cpu_used",        Collections.emptySet()))
        .put(METRICS_PREFIX + ".loaders.conns.mean",        Pair.of("loaders_conns",           Collections.emptySet()))
        .put(METRICS_PREFIX + ".loaders.memFree.mean",      Pair.of("loaders_memFree",         Collections.emptySet()))
        .put(METRICS_PREFIX + ".response.completed.median", Pair.of("response_time_median",    Collections.emptySet()))
        .put(METRICS_PREFIX + ".response.completed.upper",  Pair.of("response_time_upper",     Collections.emptySet()))
        .put(METRICS_PREFIX + ".response.completed.p95",    Pair.of("response_time_p95",       Collections.emptySet()))
        .put(METRICS_PREFIX + ".response.size.sum",         Pair.of("response_throughput_Bps", Collections.emptySet()))
        .put(METRICS_PREFIX + ".response.status.count",     Pair.of("response_status_%s_rps",  newWildcardFilter("status")))
        .put(METRICS_PREFIX + ".targets.conns.mean",        Pair.of("target_%s_conns",         newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.memFree.p95",       Pair.of("target_%s_memFree",       newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.memBuffers.p95",    Pair.of("target_%s_memBuffers",    newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.memCached.p95",     Pair.of("target_%s_memCached",     newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.cpu.median",        Pair.of("target_%s_cpu_used",      newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.load1m",            Pair.of("target_%s_load1m",        newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.load5m",            Pair.of("target_%s_load5m",        newWildcardFilter("target")))
        .put(METRICS_PREFIX + ".targets.load15m",           Pair.of("target_%s_load15m",       newWildcardFilter("target")))
        .build();

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final TypeReference<ArrayList<HashMap<String,Object>>> typeRef = new TypeReference<ArrayList<HashMap<String,Object>>>() {};

    private String newMetricNameFrom(String metricName, String keyGroup) {
        return String.format(metricsMap.get(metricName).getFirst(), keyGroup);
    }

    private Set<Filter> getFilters(String metricName) {
        return metricsMap.get(metricName).getSecond();
    }

    private Set<Filter> newWildcardFilter(String tagName) {
        return Collections.singleton(new Filter(tagName, "*", true, "wildcard"));
    }

    private ArrayList<HashMap<String, Object>> requestMetrics(Test test) {

        long testCreated = TimeUnit.MILLISECONDS.toSeconds(test.getCreatedDate().getTime());
        long testLastModified = TimeUnit.MILLISECONDS.toSeconds(test.getLastModifiedDate().getTime());
        final List<Query> queries = prepareQueries(test, testCreated, testLastModified);
        final ArrayList<HashMap<String, Object>> listOfResult = requestResults(testCreated, testLastModified, queries);

        try {
            if (listOfResult.isEmpty()) {
                return mapper.readValue("[{\"error\":\" OpenTSDB result is EMPTY \"}]", typeRef);
            } else {
                return listOfResult;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private ArrayList<HashMap<String, Object>> requestResults(long testCreated, long testLastModified, List<Query> queries) {
        final ArrayList<HashMap<String, Object>> listOfResult = new ArrayList<>();
        queries.forEach(q -> {
            ObjectNode jBody = JsonNodeFactory.instance.objectNode();
            jBody.set("queries", mapper.valueToTree(Collections.singleton(q)));
            jBody.put("start", testCreated);
            jBody.put("end", testLastModified);

            String bodyRequest = jBody.toString();
            Request requestQuery = HTTP_CLIENT.preparePost(OPENTSDB_URL + "/api/query")
                    .setBody(bodyRequest)
                    .setHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
                    .build();

            try {
                Response response = HTTP_CLIENT.executeRequest(requestQuery).get();
                String body = response.getResponseBody();
                ArrayList<HashMap<String, Object>> resultMap = mapper.readValue(body, typeRef);
                resultMap.forEach(h -> listOfResult.add(renameMetricKey(h)));
            } catch (InterruptedException | ExecutionException | IOException e) {
                if (LOGGER.isDebugEnabled()) LOGGER.error(e.getMessage(), e);
            }
        });
        return listOfResult;
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Object> renameMetricKey(HashMap<String, Object> h) {
        String status = ((Map<String, String>) h.get("tags")).get("status");
        String target = ((Map<String, String>) h.get("tags")).get("target");
        String keyGroup = status != null ? status : (target != null ? target : "");
        HashMap<String, Object> newHashMap = new HashMap<>(h);
        newHashMap.put("metric", newMetricNameFrom((String) h.get("metric"), keyGroup));
        return newHashMap;
    }

    private List<Query> prepareQueries(Test test, long testCreated, long testLastModified) {
        return metricsMap.entrySet().stream().map(Map.Entry::getKey).map(m -> {
                                        long downsampleInterval = (testLastModified - testCreated) / 2;
                                        String downsampleSuffix = "s-p95";
                                        String aggregator = "p95";
                                        return new Query(test, m, aggregator, downsampleInterval + downsampleSuffix, getFilters(m));
                                    }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Double> makeReport(Test test) {
        final TreeMap<String, Double> mapOfResult = new TreeMap<>();
        ArrayList<HashMap<String, Object>> requestMetrics = requestMetrics(test);
        if (requestMetrics != null) {
            requestMetrics.forEach(m -> {
                String key = (String) m.get("metric");
                if (key != null) {
                    Map<String, Double> dps = (Map<String, Double>) m.get("dps");
                    if (dps != null) {
                        double value = dps.entrySet().stream().mapToDouble(Map.Entry::getValue).average().orElse(-1.0);
                        mapOfResult.put(key, value);
                    }
                }
            });
            return mapOfResult;
        }
        LOGGER.error("Test {}.{}: makeReport return NULL", test.getProject(), test.getName());
        return Collections.emptyMap();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public class Filter implements Serializable {
        public final String tagk;
        public final String filter;
        public final boolean groupBy;
        public final String type;

        public Filter(String tag, String filter) {
            this(tag, filter, false, "literal_or");
        }

        public Filter(String tag, String filter, boolean groupBy, String type) {
            this.tagk = tag;
            this.filter = filter;
            this.groupBy = groupBy;
            this.type = type;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public class Query implements Serializable {
        public final String metric;
        public final String aggregator;
        public final String downsample;
        public final Filter[] filters;

        public Query(Test test, String metric, String aggregator, String downsample, Set<Filter> otherFilters) {
            this.metric = metric;
            this.aggregator = aggregator;
            this.downsample = downsample;
            final ArrayList<Filter> allFilter = new ArrayList<>(Arrays.asList(
                    new Filter("project", test.getProject()),
                    new Filter("test", test.getName())));
            allFilter.addAll(otherFilters);
            filters = allFilter.toArray(new Filter[allFilter.size()]);
        }
    }

}
