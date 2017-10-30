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
import com.globocom.grou.exceptions.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
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
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class TestEventListener extends AbstractMongoEventListener<Test> {

    private static final String TEST_QUEUE = "grou:test_queue";
    private static final String GROU_LOCK  = "grou:lock";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventListener.class);
    private static final String ABORT_PREFIX = "ABORT:";

    private final ObjectMapper mapper = new ObjectMapper();
    private final BrowserCallback<Integer> browserCallback = TestEventListener::doInJms;
    private final Pageable allPages = new PageRequest(0, 99999);

    private final TestRepository testRepository;
    private final StringRedisTemplate redisTemplate;
    private final JmsTemplate jmsTemplate;
    private final LockerService lockerService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    public TestEventListener(TestRepository testRepository, StringRedisTemplate redisTemplate, JmsTemplate jmsTemplate, LockerService lockerService) {
        this.testRepository = testRepository;
        this.redisTemplate = redisTemplate;
        this.jmsTemplate = jmsTemplate;
        this.lockerService = lockerService;
    }

    @Override
    public void onBeforeSave(BeforeSaveEvent<Test> event) {
        Test test = event.getSource();
        if (test.getStatus() == Test.Status.SCHEDULED) {
            int limitRateSeconds = Integer.parseInt(SystemEnv.REQUESTS_LIMIT.getValue());
            LOGGER.info("Checking request limit (minimum interval between tests to project {} is {} seconds)", test.getProject(), limitRateSeconds);
            if (limitRateSeconds > 0) {
                String project = test.getProject();
                String timeZone = SystemEnv.MONGO_TIMEZONE.getValue();
                int offset = ZoneId.of(timeZone).getRules().getOffset(Instant.now()).getTotalSeconds() - limitRateSeconds;
                Instant instantOffset = Instant.now().plusSeconds(offset);
                Page<Test> pageTest = testRepository.findByProjectAndCreatedDateAfter(project, instantOffset, allPages);
                if (pageTest.hasContent()) {
                    LOGGER.warn("Test {}.{} ignored. requests limit exceeded (minimum allowed interval between requests is {} seconds).",
                            test.getProject(), test.getName(), limitRateSeconds);
                    throw new ForbiddenException("Requests limit exceeded. Try again in " + limitRateSeconds + " seconds.");
                }
            }
        }
    }

    @Override
    public void onAfterSave(AfterSaveEvent<Test> event) {
        Test test = event.getSource();
        if (test.getStatus() == Test.Status.SCHEDULED) {
            try {
                if (lockerService.lockJms()) {
                    test.setStatus(Test.Status.ENQUEUED);
                    testRepository.save(test);
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
            Integer parallelLoadersProperty = Math.max(1, (Integer) (Optional.ofNullable(test.getProperties().get("parallelLoaders")).orElse(1)));
            Set<Loader> loadersIdle = getLoadersIdle(parallelLoadersProperty);
            if (loadersIdle.size() == parallelLoadersProperty) {
                test.setLoaders(new HashSet<>(loadersIdle));
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
                        callbackError(test, Test.Status.ABORTED.toString());
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

    private Set<Loader> getLoadersIdle(int parallelLoadersProperty) {
        return redisTemplate.keys("grou:loader:*").stream()
                .map(redisKey -> {
                    try {
                        String jsonStr = redisTemplate.opsForValue().get(redisKey);
                        return mapper.readValue(jsonStr, Loader.class);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                        return null;
                    }
                })
                .filter(loader -> loader != null && Loader.Status.IDLE.equals(loader.getStatus()))
                .sorted(Comparator.comparingLong(l -> l.getLastExecAt().getTime()))
                .limit(parallelLoadersProperty)
                .collect(Collectors.toSet());
    }

    private boolean isAbort(String testNameWithProject) {
        Set<String> keys = redisTemplate.keys(ABORT_PREFIX + testNameWithProject + "*");
        keys.forEach(key -> redisTemplate.expire(key, 10000, TimeUnit.MILLISECONDS));
        return !keys.isEmpty();
    }

    private void callbackError(Test test, String errorDetailed) throws JsonProcessingException {
        Loader undefLoader = new Loader();
        undefLoader.setName("UNDEF");
        undefLoader.setStatus(Loader.Status.ERROR);
        undefLoader.setStatusDetailed(errorDetailed);
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
}
