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
import com.mongodb.MongoClientURI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.Optional;

@Configuration
public class MongoConfiguration {

    private static final String MONGO_CONN = Optional.ofNullable(System.getenv("MONGO_CONN")).orElse("mongodb://@localhost:27017/grou");

    @Bean
    public Mongo mongo() throws UnknownHostException {
        return Mongo.Holder.singleton().connect(new MongoClientURI(MONGO_CONN));
    }
}
