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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

@Configuration
public class MongoConfiguration extends AbstractMongoConfiguration {

    private static final String MONGO_HOST = Optional.ofNullable(System.getenv("MONGO_HOST")).orElse("localhost");
    private static final String MONGO_PORT = Optional.ofNullable(System.getenv("MONGO_PORT")).orElse("27017");
    private static final String MONGO_DB   = Optional.ofNullable(System.getenv("MONGO_DB")).orElse("grou");
    private static final String MONGO_USER = System.getenv("MONGO_USER");
    private static final String MONGO_PASS = System.getenv("MONGO_PASS");

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    protected String getDatabaseName() {
        return MONGO_DB;
    }

    @Override
    public Mongo mongo() throws Exception {
        log.info(String.format("MONGO_HOST: %s, MONGO_PORT: %s, MONGO_DB: %s", MONGO_HOST, MONGO_PORT, MONGO_DB));
        final List<MongoCredential> credentialsList = MONGO_USER == null || MONGO_PASS == null ? Collections.emptyList() :
                singletonList(MongoCredential.createCredential(MONGO_USER, MONGO_DB, MONGO_PASS.toCharArray()));
        return new MongoClient(singletonList(new ServerAddress(MONGO_HOST, Integer.valueOf(MONGO_PORT))), credentialsList);
    }

}
