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

package com.globocom.grou.configurations;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.globocom.grou.SystemEnv.*;
import static java.util.Collections.singletonList;

@Configuration
@EnableMongoAuditing
public class MongoConfiguration extends AbstractMongoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfiguration.class);

    @Override
    protected String getDatabaseName() {
        return MONGO_DB.getValue();
    }

    @Override
    public Mongo mongo() throws Exception {
        LOGGER.info("MONGO_HOST: {}, MONGO_PORT: {}, MONGO_DB: {}", MONGO_HOST.getValue(), MONGO_PORT.getValue(), MONGO_DB.getValue());
        final List<MongoCredential> credentialsList = "".equals(MONGO_USER.getValue()) || "".equals(MONGO_PASS.getValue()) ? Collections.emptyList() :
                singletonList(MongoCredential.createCredential(MONGO_USER.getValue(), MONGO_DB.getValue(), MONGO_PASS.getValue().toCharArray()));

        if ("".equals(MONGO_SERVERS.getValue())) {
            return new MongoClient(singletonList(new ServerAddress(MONGO_HOST.getValue(), Integer.parseInt(MONGO_PORT.getValue()))), credentialsList);
        } else {
            return new MongoClient(Arrays.stream(MONGO_SERVERS.getValue().split(",")).map(String::trim).map(s -> {
                int idx;
                String host = s;
                int port;
                if ((idx = s.indexOf(":")) > -1) {
                    host = s.substring(0, idx);
                    port = Integer.parseInt(s.substring(idx + 1));
                } else {
                    port = 27017;
                }
                return new ServerAddress(host, port);
            }).collect(Collectors.toList()));
        }
    }

}
