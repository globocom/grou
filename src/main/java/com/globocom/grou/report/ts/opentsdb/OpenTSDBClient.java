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
import com.google.common.util.concurrent.AtomicDouble;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class OpenTSDBClient implements TSClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTSDBClient.class);

    private static final int NUM_SAMPLES = 10000;

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

    private final List<Object[]> metricsMap = Arrays.asList(
                    // metric name (opentsdb)                      // metric name for humans        // aggr // ds  // group by
        new Object[]{ METRICS_PREFIX + ".loaders.cpu.mean",          "Loaders %s cpu used (%%)",      "p95", "p95", newWildcardFilter("loader")},
        new Object[]{ METRICS_PREFIX + ".loaders.conns.mean",        "Loaders %s connections",        "p95", "p95", newWildcardFilter("loader")},
        new Object[]{ METRICS_PREFIX + ".loaders.memFree.mean",      "Loaders %s memFree (Mbytes)",   "p95", "p95", newWildcardFilter("loader")},
        new Object[]{ METRICS_PREFIX + ".response.completed.median", "Response time (ms) - median",   "p95", "p95", Collections.emptySet()},
        new Object[]{ METRICS_PREFIX + ".response.completed.upper",  "Response time (ms) - upper",    "p95", "p95", Collections.emptySet()},
        new Object[]{ METRICS_PREFIX + ".response.completed.p95",    "Response time (ms) - p95",      "p95", "p95", Collections.emptySet()},
        new Object[]{ METRICS_PREFIX + ".response.size.sum",         "Response throughput (Kbyte/s)", "p95", "p95", Collections.emptySet()},
        new Object[]{ METRICS_PREFIX + ".response.status.count",     "Response %s",                   "sum", "sum", newWildcardFilter("status")},
        new Object[]{ METRICS_PREFIX + ".targets.conns.mean",        "Target %s conns (avg)",         "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.memFree.p95",       "Target %s memFree (Mbytes)",    "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.memBuffers.p95",    "Target %s memBuffers (Mbytes)", "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.memCached.p95",     "Target %s memCached (Mbytes)",  "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.cpu.median",        "Target %s cpu used (%%)",       "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.iowait.median",     "Target %s cpu iowait (%%)",     "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.steal.median",      "Target %s cpu steal (%%)",      "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.irq.median",        "Target %s cpu irq (%%)",        "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.softirq.median",    "Target %s cpu softirq (%%)",    "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.load1m",            "Target %s load 1 min",          "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.load5m",            "Target %s load 5 min",          "p95", "p95", newWildcardFilter("target")},
        new Object[]{ METRICS_PREFIX + ".targets.load15m",           "Target %s load 15 min",         "p95", "p95", newWildcardFilter("target")}
        );

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final TypeReference<ArrayList<HashMap<String,Object>>> typeRef = new TypeReference<ArrayList<HashMap<String,Object>>>() {};

    @SuppressWarnings("ConstantConditions")
    private String metricHumanName(String metricName, String keyGroup, String aggr, String ds) {
        String humanNameWithPattern = metricsMap.stream()
                                        .filter(e -> metricName.equals(e[0]) && aggr.equals(e[2]) && ds.endsWith((String) e[3]))
                                        .map(e -> (String) e[1])
                                        .findAny().orElse("METRIC_NOT_FOUND");
        return String.format(humanNameWithPattern, keyGroup);
    }

    private Set<Filter> newWildcardFilter(String tagName) {
        return Collections.singleton(new Filter(tagName, "*", true, "wildcard"));
    }

    private ArrayList<HashMap<String, Object>> metrics(Test test) {
        long testCreated = TimeUnit.MILLISECONDS.toSeconds(test.getCreatedDate().getTime());
        long testLastModified = TimeUnit.MILLISECONDS.toSeconds(test.getLastModifiedDate().getTime());
        final List<Query> queries = prepareQueries(test, testCreated, testLastModified);
        final ArrayList<HashMap<String, Object>> listOfResult = doRequest(testCreated, testLastModified, queries);

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

    private ArrayList<HashMap<String, Object>> doRequest(long testCreated, long testLastModified, List<Query> queries) {
        final ArrayList<HashMap<String, Object>> listOfResult = new ArrayList<>();
        queries.forEach(query -> {
            String aggr = query.aggregator;
            String ds = query.downsample;
            ObjectNode jBody = JsonNodeFactory.instance.objectNode();
            jBody.set("queries", mapper.valueToTree(Collections.singleton(query)));
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
                for (HashMap<String, Object> result : resultMap) result.put("aggr", aggr);
                resultMap.forEach(h -> listOfResult.add(renameMetricKey(h, aggr, ds)));
            } catch (InterruptedException | ExecutionException | IOException e) {
                if (LOGGER.isDebugEnabled()) LOGGER.error(e.getMessage(), e);
            }
        });
        return listOfResult;
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Object> renameMetricKey(HashMap<String, Object> resultMap, String aggr, String ds) {
        String status = ((Map<String, String>) resultMap.get("tags")).get("status");
        String target = ((Map<String, String>) resultMap.get("tags")).get("target");
        String loader = ((Map<String, String>) resultMap.get("tags")).get("loader");
        String keyGroup = status != null ? status : (target != null ? target : (loader != null ? loader : ""));
        HashMap<String, Object> newHashMap = new HashMap<>(resultMap);
        newHashMap.put("metric", metricHumanName((String) resultMap.get("metric"), keyGroup, aggr, ds));
        return newHashMap;
    }

    @SuppressWarnings("unchecked")
    private List<Query> prepareQueries(Test test, long testCreated, long testLastModified) {
        return metricsMap.stream().map(m ->
        {
            long downsampleInterval = Math.max(1, (testLastModified - testCreated) / NUM_SAMPLES);
            String metric = (String) m[0];
            String aggregator = (String) m[2];
            String downsampleAlgorithm = (String) m[3];
            String secondSuffix = "s";
            String downsampleSuffix = secondSuffix + "-" + downsampleAlgorithm;
            String downsample = downsampleInterval + downsampleSuffix;
            Set<Filter> otherFilters = (Set<Filter>) m[4];
            return new Query(test, metric, aggregator, downsample, otherFilters);
        }).collect(Collectors.toList());
    }

    private double formatValue(double value) {
        try {
            return BigDecimal.valueOf(value).round(new MathContext(4, RoundingMode.HALF_UP)).doubleValue();
        } catch (NumberFormatException ignore) {
            return BigDecimal.valueOf(0d).round(new MathContext(4, RoundingMode.HALF_UP)).doubleValue();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Double> makeReport(Test test) {
        final TreeMap<String, Double> mapOfResult = new TreeMap<>();
        ArrayList<HashMap<String, Object>> metrics = Optional.ofNullable(metrics(test)).orElse(new ArrayList<>());
        metrics.stream().filter(metric -> Objects.nonNull(metric.get("metric"))).forEach(metric -> {
            String key = (String) metric.get("metric");
            String aggr = (String) metric.get("aggr");
            int durationTimeMillis = test.getDurationTimeMillis();
            Map<String, Double> dps = Optional.ofNullable((Map<String, Double>) metric.get("dps")).orElse(Collections.emptyMap());
            final AtomicDouble reduceSum = new AtomicDouble(0.0);
            final AtomicDouble reduceMax = new AtomicDouble(0.0);
            dps.entrySet().stream().mapToDouble(Map.Entry::getValue).forEach(delta -> {
                reduceSum.addAndGet(delta);
                if (reduceMax.get() < delta) reduceMax.set(delta);
            });
            double value = reduceSum.get();
            double max = reduceMax.get();
            if (!Double.isNaN(value)) {
                if ("sum".equals(aggr)) {
                    int durationTimeSecs = durationTimeMillis / 1000;
                    double avg = value / (double) durationTimeSecs;
                    mapOfResult.put(key + " (total)", formatValue(value));
                    mapOfResult.put(key + " (avg tps)", formatValue(avg));
                    mapOfResult.put(key + " (max tps)", formatValue(max / Math.max(1.0, (double) durationTimeSecs / (double) NUM_SAMPLES)));
                } else {
                    value = value / (double) dps.size();
                    mapOfResult.put(key, formatValue(value));
                }
            }
        });
        if (mapOfResult.isEmpty()) LOGGER.error("Test {}.{}: makeReport return NULL", test.getProject(), test.getName());
        return mapOfResult;
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
