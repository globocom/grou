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

package com.globocom.grou.entities.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globocom.grou.SystemEnv;
import com.globocom.grou.entities.Loader;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.events.services.CallbackListenerService;
import com.globocom.grou.entities.repositories.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class TestEventListener extends AbstractMongoEventListener<Test> {

    private static final String TEST_QUEUE = "grou:test_queue";
    private static final String GROU_LOCK  = "grou:lock";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final TestRepository testRepository;
    private final StringRedisTemplate redisTemplate;
    private final JmsTemplate jmsTemplate;

    @Autowired
    public TestEventListener(TestRepository testRepository, StringRedisTemplate redisTemplate, JmsTemplate jmsTemplate) {
        this.testRepository = testRepository;
        this.redisTemplate = redisTemplate;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void onAfterSave(AfterSaveEvent<Test> event) {
        if (redisTemplate.opsForValue().setIfAbsent(GROU_LOCK, UUID.randomUUID().toString())) {
            redisTemplate.expire(GROU_LOCK, 10, TimeUnit.SECONDS);
            Test test = event.getSource();
            if (test.getStatus() == Test.Status.SCHEDULED) {
                try {
                    jmsTemplate.convertAndSend(TEST_QUEUE, mapper.writeValueAsString(test));
                } catch (JsonProcessingException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } else {
            LOGGER.warn(GROU_LOCK + " found. Ignoring event.");
        }
    }

    @JmsListener(destination = TEST_QUEUE, concurrency = "1-1")
    public void consumer(String testStr, @Headers final Map<String, Integer> jmsHeaders) throws IOException, InterruptedException {
        Thread.sleep(Long.parseLong(SystemEnv.CONSUMER_PAUSE.getValue()));
        final Test test = mapper.readValue(testStr, Test.class);
        try {
            test.setStatus(Test.Status.ENQUEUED);
            testRepository.save(test);
            final AtomicInteger loadersRunning = new AtomicInteger(0);
            final AtomicInteger parallelLoaders = new AtomicInteger(1);
            Integer parallelLoadersProperty = (Integer) (Optional.ofNullable(test.getProperties().get("parallelLoaders")).orElse(1));
            Predicate<String> onlyLoadersUndef = loaderKey -> Test.Status.UNDEF.toString().equals(redisTemplate.opsForValue().get(loaderKey));
            List<String> loadersUndef = redisTemplate.keys("grou:loader:*").stream().filter(onlyLoadersUndef).collect(Collectors.toList());

            if (loadersUndef.size() >= parallelLoadersProperty) {
                loadersUndef.forEach(k -> {
                    if (loadersRunning.incrementAndGet() <= parallelLoaders.get()) {
                        String loaderName = k.split(":")[2];
                        String channel = TEST_QUEUE + ":" + loaderName;
                        redisTemplate.convertAndSend(channel, testStr);
                        LOGGER.info("Test {} sent to channel {}", test.getProject() + "." + test.getName(), channel);
                    }
                });
            } else {
                final AtomicInteger retry = new AtomicInteger(Optional.ofNullable(jmsHeaders.get("retry")).orElse(1));
                String uniqRef = test.getProject() + "." + test.getName();
                String errorDetailed = "Insufficient loaders (only " + loadersUndef.size() + " available [" + parallelLoadersProperty + " required]).";
                int maxRetry = Integer.parseInt(SystemEnv.MAX_RETRY.getValue());
                if (retry.get() <= maxRetry) {
                    LOGGER.warn("Test " + uniqRef + ": " + errorDetailed + " Re-queuing (retry " + retry.get() + "/" + maxRetry + ").");

                    jmsTemplate.convertAndSend(TEST_QUEUE, testStr, message -> {
                        message.setIntProperty("retry", retry.incrementAndGet());
                        return message;
                    });
                } else {
                    Loader undefLoader = new Loader();
                    undefLoader.setName("UNDEF");
                    undefLoader.setStatus(Test.Status.ERROR);
                    undefLoader.setStatusDetailed(errorDetailed);
                    test.setLoaders(Collections.singleton(undefLoader));
                    redisTemplate.convertAndSend(CallbackListenerService.CALLBACK_QUEUE, mapper.writeValueAsString(test));
                    LOGGER.error("Test " + uniqRef + ": " + errorDetailed + " Dropped.");
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            redisTemplate.delete(GROU_LOCK);
        }

    }

}
