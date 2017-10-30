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

package com.globocom.grou.report.ts;

import com.globocom.grou.entities.Test;
import com.globocom.grou.report.ts.opentsdb.OpenTSDBClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface TSClient {

    enum Type {
        OPENTSDB(new OpenTSDBClient());

        public final TSClient INSTANCE;

        Type(TSClient tsClient) {
            this.INSTANCE = tsClient;
        }
    }

    Map<String, String> metricsNameConverted();

    ArrayList<HashMap<String, Object>> makeReport(Test test);
}
