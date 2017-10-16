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
import com.globocom.grou.entities.events.services.LockerService;
import com.globocom.grou.entities.repositories.TestRepository;
import com.globocom.grou.entities.services.LoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class TestEventListener extends AbstractMongoEventListener<Test> {

    private static final String TEST_QUEUE = "grou:test_queue";
    private static final String GROU_LOCK  = "grou:lock";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventListener.class);
    private static final String ABORT_PREFIX = "ABORT:";

    private final ObjectMapper mapper = new ObjectMapper();
    private final BrowserCallback<Integer> browserCallback = TestEventListener::doInJms;

    private final TestRepository testRepository;
    private final StringRedisTemplate redisTemplate;
    private final JmsTemplate jmsTemplate;
    private final LockerService lockerService;
    private final LoaderService loaderService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    public TestEventListener(TestRepository testRepository, StringRedisTemplate redisTemplate, JmsTemplate jmsTemplate, LockerService lockerService, LoaderService loaderService) {
        this.testRepository = testRepository;
        this.redisTemplate = redisTemplate;
        this.jmsTemplate = jmsTemplate;
        this.lockerService = lockerService;
        this.loaderService = loaderService;
    }

    @Override
    public void onAfterSave(AfterSaveEvent<Test> event) {
        Test test = event.getSource();
        if (test.getStatus() == Test.Status.SCHEDULED) {
            try {
                if (lockerService.lockJms()) {
                    jmsTemplate.convertAndSend(TEST_QUEUE, mapper.writeValueAsString(test));
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                lockerService.releaseLockJms();
            }
        }
    }

    @JmsListener(destination = TEST_QUEUE, concurrency = "1-1")
    public void consumer(String testStr, @Headers final Map<String, Integer> jmsHeaders) throws IOException, InterruptedException {
        Thread.sleep(Long.parseLong(SystemEnv.CONSUMER_PAUSE.getValue()));
        final Test test = mapper.readValue(testStr, Test.class);
        try {
            test.setStatus(Test.Status.ENQUEUED);
            testRepository.save(test);
            Integer parallelLoadersProperty = Math.max(1, (Integer) (Optional.ofNullable(test.getProperties().get("parallelLoaders")).orElse(1)));
            Predicate<String> onlyLoadersIdle = loaderKey -> Loader.Status.IDLE.toString().equals(redisTemplate.opsForValue().get(loaderKey));
            Set<String> loadersIdle = redisTemplate.keys("grou:loader:*").stream().filter(onlyLoadersIdle).limit(parallelLoadersProperty).collect(Collectors.toSet());
            if (loadersIdle.size() == parallelLoadersProperty) {
                test.setLoaders(loadersIdle.stream().map(k -> k.split(":")[2]).map(this::newLoader).collect(Collectors.toSet()));
                testRepository.save(test);
                test.getLoaders().forEach(loader -> {
                    try {
                        String channel = TEST_QUEUE + ":" + loader.getName();
                        redisTemplate.convertAndSend(channel, mapper.writeValueAsString(test));
                        LOGGER.info("Test {} sent to channel {}", test.getProject() + "." + test.getName(), channel);
                    } catch (JsonProcessingException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            } else {
                final AtomicInteger retry = new AtomicInteger(Optional.ofNullable(jmsHeaders.get("retry")).orElse(1));
                String testNameWithProject = test.getProject() + "." + test.getName();
                String errorDetailed = "Insufficient loaders (available=" + loadersIdle.size() + ", required=" + parallelLoadersProperty + ").";
                int maxRetry = Integer.parseInt(SystemEnv.MAX_RETRY.getValue());
                if (retry.get() <= maxRetry) {
                    if (isAbort(testNameWithProject)) {
                        loaderService.loaders().stream().map(Loader::getName).forEach(loaderName ->
                            redisTemplate.expire(ABORT_PREFIX + testNameWithProject + "#" + loaderName, 10, TimeUnit.MILLISECONDS)
                        );
                        callbackError(test, "Aborted.");
                    } else {
                        LOGGER.warn("Test {}: {} Re-queued (retry {}/{}).", testNameWithProject, errorDetailed, retry.get(), maxRetry);
                        jmsTemplate.convertAndSend(TEST_QUEUE, testStr, message -> {
                            message.setIntProperty("retry", retry.incrementAndGet());
                            return message;
                        });
                        LOGGER.warn(">> Waiting in queue: " + jmsTemplate.browseSelected(TEST_QUEUE, "retry IS NOT NULL", browserCallback));
                    }
                } else {
                    callbackError(test, errorDetailed + " Dropped.");
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            redisTemplate.delete(GROU_LOCK);
        }

    }

    private boolean isAbort(String testNameWithProject) {
        Set<String> keys = redisTemplate.keys(ABORT_PREFIX + testNameWithProject + "*");
        return keys != null && !keys.isEmpty();
    }

    private void callbackError(Test test, String errorDetailed) throws JsonProcessingException {
        Loader undefLoader = newLoader("UNDEF", Loader.Status.ERROR, errorDetailed);
        test.setLoaders(Collections.singleton(undefLoader));
        redisTemplate.convertAndSend(CallbackListenerService.CALLBACK_QUEUE, mapper.writeValueAsString(test));
        LOGGER.error("Test {}.{}: {}", test.getProject(), test.getName(), errorDetailed);
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.warn("Shutdown: Removing {} lock.", GROU_LOCK);
        redisTemplate.delete(GROU_LOCK);
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static Integer doInJms(Session s, QueueBrowser qb) throws JMSException {
        return Collections.list(qb.getEnumeration()).size();
    }

    private Loader newLoader(String name) {
        return newLoader(name, null, null);
    }

    private Loader newLoader(String name, Loader.Status status, String statusDetailed) {
        Loader loader = new Loader();
        loader.setName(name);
        loader.setStatus(status != null ? status : Loader.Status.IDLE);
        loader.setStatusDetailed(statusDetailed != null ? statusDetailed : "");
        return loader;
    }
}
