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

import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Collections;
import java.util.List;

import static com.globocom.grou.SystemEnv.*;
import static java.util.Collections.singletonList;

@Configuration
@EnableMongoAuditing
public class MongoConfiguration extends AbstractMongoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfiguration.class);

    private MongoClientURI mongoClientUri = null;

    @Override
    protected String getDatabaseName() {
        if (getMongoClientUri() != null) return mongoClientUri.getDatabase();

        return MONGO_DB.getValue();
    }

    @Override
    public Mongo mongo() throws Exception {
        if ("".equals(MONGO_URI.getValue())) {
            LOGGER.info("MONGO_HOST: {}, MONGO_PORT: {}, MONGO_DB: {}", MONGO_HOST.getValue(), MONGO_PORT.getValue(), MONGO_DB.getValue());
            final List<MongoCredential> credentialsList = "".equals(MONGO_USER.getValue()) || "".equals(MONGO_PASS.getValue()) ? Collections.emptyList() :
                    singletonList(MongoCredential.createCredential(MONGO_USER.getValue(), MONGO_DB.getValue(), MONGO_PASS.getValue().toCharArray()));
            return new MongoClient(singletonList(new ServerAddress(MONGO_HOST.getValue(), Integer.parseInt(MONGO_PORT.getValue()))), credentialsList);
        } else {
            LOGGER.info("MONGO_URI: {}", MONGO_URI.getValue().replaceAll("([/,]).*@", "$1xxxx:xxxx@"));
            return new MongoClient(getMongoClientUri());
        }
    }

    private synchronized MongoClientURI getMongoClientUri() {
        if (mongoClientUri == null && !"".equals(MONGO_URI.getValue())) {
            mongoClientUri = new MongoClientURI(MONGO_URI.getValue());
        }
        return mongoClientUri;
    }
}
