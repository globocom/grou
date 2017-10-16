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

package com.globocom.grou;

import java.util.Optional;

/**
 * The enum System environments.
 */
@SuppressWarnings("unused")
public enum SystemEnv {

    /**
     * Keystone url.
     */
    KEYSTONE_URL ("OS_AUTH_URL", "http://controller:5000/v3"),

    /**
     * Keystone domain context.
     */
    KEYSTONE_DOMAIN_CONTEXT ("OS_PROJECT_DOMAIN_NAME","grou"),

    /**
     * Mongo servers (ip:port format, comma separator)
     */
    MONGO_SERVERS ("MONGO_SERVERS", ""),

    /**
     * Mongo host.
     */
    MONGO_HOST  ("MONGO_HOST", "localhost"),

    /**
     * Mongo port.
     */
    MONGO_PORT  ("MONGO_PORT", "27017"),

    /**
     * Mongo db.
     */
    MONGO_DB    ("MONGO_DB", "grou"),

    /**
     * Mongo user.
     */
    MONGO_USER  ("MONGO_USER", ""),

    /**
     * Mongo pass.
     */
    MONGO_PASS  ("MONGO_PASS", ""),

    /**
     * Redis hostname.
     */
    REDIS_HOSTNAME ("REDIS_HOSTNAME", "127.0.0.1"),

    /**
     * Redis port.
     */
    REDIS_PORT ("REDIS_PORT", 6379),

    /**
     * Redis password.
     */
    REDIS_PASSWORD ("REDIS_PASSWORD", ""),

    /**
     * Redis database.
     */
    REDIS_DATABASE ("REDIS_DATABASE", ""),

    /**
     * Redis use sentinel.
     */
    REDIS_USE_SENTINEL ("REDIS_USE_SENTINEL", Boolean.FALSE),

    /**
     * Redis sentinel master name.
     */
    REDIS_SENTINEL_MASTER_NAME ("REDIS_SENTINEL_MASTER_NAME", "mymaster"),

    /**
     * Redis sentinel nodes.
     */
    REDIS_SENTINEL_NODES ("REDIS_SENTINEL_NODES", "127.0.0.1:26379"),

    /**
     * Redis max idle.
     */
    REDIS_MAXIDLE  ("REDIS_MAXIDLE", "100"),

    /**
     * Redis timeout (ms).
     */
    REDIS_TIMEOUT  ("REDIS_TIMEOUT", "60000"),

    /**
     * Redis max total.
     */
    REDIS_MAXTOTAL ("REDIS_MAXTOTAL", "128"),

    /**
     * Re-queue max retry.
     */
    MAX_RETRY ("MAX_RETRY", 10),

    /**
     * Queue Consumer pause (ms).
     */
    CONSUMER_PAUSE ("CONSUMER_PAUSE", 5000L),

    /**
     * Dashboard URL
     */
    DASHBOARD_URL ("DASHBOARD_URL", "http://localhost:3000/dashboard/db/grou");

    /**
     * Gets SystemEnv value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    private final String value;

    SystemEnv(String env, Object def) {
        this.value = Optional.ofNullable(System.getenv(env)).orElse(String.valueOf(def));
    }
}
