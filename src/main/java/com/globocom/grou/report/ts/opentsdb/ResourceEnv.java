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

import java.util.Optional;

public enum ResourceEnv {

    /**
     * OpenTSDB URL
     */
    OPENTSDB_URL("OPENTSDB_URL", "http://localhost:4242");

    /**
     * Gets Env value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    private final String value;

    ResourceEnv(String env, Object def) {
        this.value = Optional.ofNullable(System.getenv(env)).orElse(String.valueOf(def));
    }
}
