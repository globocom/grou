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

import java.util.Collections;
import java.util.List;

import static com.globocom.grou.SystemEnv.MONGO_DB;
import static com.globocom.grou.SystemEnv.MONGO_HOST;
import static com.globocom.grou.SystemEnv.MONGO_PASS;
import static com.globocom.grou.SystemEnv.MONGO_PORT;
import static com.globocom.grou.SystemEnv.MONGO_USER;
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
        return new MongoClient(singletonList(new ServerAddress(MONGO_HOST.getValue(), Integer.parseInt(MONGO_PORT.getValue()))), credentialsList);
    }

}
