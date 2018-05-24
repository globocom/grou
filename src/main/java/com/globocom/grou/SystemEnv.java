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
public enum SystemEnv {

    /**
     * Keystone url.
     */
    KEYSTONE_URL ("OS_AUTH_URL", "http://127.0.0.1:5000/v3"),

    /**
     * Keystone domain context.
     */
    KEYSTONE_DOMAIN_CONTEXT ("OS_PROJECT_DOMAIN_NAME","grou"),

    /**
     * Mongo URI client (URI format)
     */
    MONGO_URI ("MONGO_URI", ""),

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
     * MongoDB TimeZone ID
     */
    MONGO_TIMEZONE("MONGO_TIMEZONE", "Z"),

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
    DASHBOARD_URL ("DASHBOARD_URL", "http://localhost:3000/dashboard/db/grou"),

    /**
     * Project admins scope
     */
    PROJECT_ADMIN("PROJECT_ADMIN", "grouadmins"),

    /**
     * Mail From (when using ReportService)
     */
    REPORT_MAIL_FROM("REPORT_MAIL_FROM", "root@localhost"),

    /**
     * TimeSeries DB type
     */
    TS_TYPE("TS_TYPE", "OPENTSDB"),

    /**
     * TimeSeries metrics prefix
     */
    TS_METRIC_PREFIX("TS_METRIC_PREFIX", "grou"),

    /**
     * DDOS protection. API requests limit (interval mandatory minimum between requests per project).
     */
    REQUESTS_LIMIT("REQUESTS_LIMIT", -1),

    /**
     * Mail host. Example: smtp.gmail.com
     */
    MAIL_HOST("MAIL_HOST", ""),

    /**
     * Mail service port. Example: 587
     */
    MAIL_PORT("MAIL_PORT", 25),

    /**
     * Mail transport. Default "smtp".
     */
    MAIL_TRANSPORT("MAIL_TRANSPORT", "smtp"),

    /**
     * Mail enable TLS support
     */
    MAIL_TLS("MAIL_TLS", "false"),

    /**
     * Mail login user.
     */
    MAIL_USER("MAIL_USER", ""),

    /**
     * Mail password.
     */
    MAIL_PASS("MAIL_PASS", "");

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
