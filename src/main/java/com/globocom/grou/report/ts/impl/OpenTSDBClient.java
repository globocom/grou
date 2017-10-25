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

package com.globocom.grou.report.ts.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.globocom.grou.entities.Test;
import com.globocom.grou.report.ts.TSClient;

public class OpenTSDBClient implements TSClient {

    private ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String makeReport(Test test) {

        // TODO: get metrics from TSDB and summarize
        /**
         * DS='30s-p95';
         * curl -H'content-type: application/json' -d \
         * '{
         *      "queries": [
         *          {
         *              "metric": "grou.response.status.count",
         *              "aggregator": "avg",
         *              "downsample": "$DS",
         *              "filters":[
         *                  {
         *                      "filter": "$TEST",
         *                      "groupBy": false,
         *                      "tagk": "test",
         *                      "type": "literal_or"
         *                  }]
         *           }],
         *      "start": 1508879952000,
         *      "end": 1508879997000
         * }' \
         * http://metrics.grou.globoi.com:4242/api/query
         *
         * # response
         *
         *  [
         *  {
         *    "metric": "grou.response.status.count",
         *    "tags": {
         *      "project": "$PROJECT",
         *      "source": "statsd",
         *      "test": "$TEST",
         *      "alltags": "UNDEF"
         *    },
         *    "aggregateTags": [
         *      "loader",
         *      "status"
         *    ],
         *    "dps": {
         *      "1508879970": 71801.6
         *    }
         *  }
         * ]
         *
         */
        try {
            return mapper.writeValueAsString(test);
        } catch (JsonProcessingException e) {
            return "{error:" + e.getMessage() + "}";
        }
    }
}
