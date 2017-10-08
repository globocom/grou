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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.globocom.grou.SystemEnv.*;
import static com.globocom.grou.entities.events.services.CallbackListenerService.CALLBACK_QUEUE;

@Configuration
public class RedisConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(CALLBACK_QUEUE));
        return container;
    }

    @Bean
    public RedisConnectionFactory connectionFactory() {
        JedisConnectionFactory jedisConnectionFactory;
        if (Boolean.valueOf(REDIS_USE_SENTINEL.getValue())) {
            List<String> redisSentinelNodesStringList = Arrays.asList(REDIS_SENTINEL_NODES.getValue().split(","));
            Iterable<RedisNode> redisSentinelNodesList = redisSentinelNodesStringList.stream()
                    .map(node -> new RedisNode(node.split(":")[0], Integer.parseInt(node.split(":")[1])))
                    .collect(Collectors.toList());
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            sentinelConfig.master(REDIS_SENTINEL_MASTER_NAME.getValue()).setSentinels(redisSentinelNodesList);
            jedisConnectionFactory = new JedisConnectionFactory(sentinelConfig);
            jedisConnConfig(jedisConnectionFactory);
        } else {
            jedisConnectionFactory = new JedisConnectionFactory();
            jedisConnectionFactory.setHostName(REDIS_HOSTNAME.getValue());
            jedisConnConfig(jedisConnectionFactory);
            jedisConnectionFactory.setPort(Integer.parseInt(REDIS_PORT.getValue()));
        }

        return jedisConnectionFactory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private void jedisConnConfig(final JedisConnectionFactory jedisConnectionFactory) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        try {
            poolConfig.setMaxTotal(Integer.parseInt(REDIS_MAXTOTAL.getValue()));
            poolConfig.setMaxIdle(Integer.parseInt(REDIS_MAXIDLE.getValue()));
            poolConfig.setBlockWhenExhausted(true);
            if (!"".equals(REDIS_PASSWORD.getValue())) {
                jedisConnectionFactory.setPassword(REDIS_PASSWORD.getValue());
            }
            if (!"".equals(REDIS_DATABASE.getValue())) {
                jedisConnectionFactory.setDatabase(Integer.parseInt(REDIS_DATABASE.getValue()));
            }
            jedisConnectionFactory.setPoolConfig(poolConfig);
            jedisConnectionFactory.setUsePool(true);
            jedisConnectionFactory.setTimeout(Integer.parseInt(REDIS_TIMEOUT.getValue()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
