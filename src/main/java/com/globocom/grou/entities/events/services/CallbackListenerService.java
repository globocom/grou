package com.globocom.grou.entities.events.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globocom.grou.entities.Loader;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CallbackListenerService {

    public static final String CALLBACK_QUEUE = "grou:test_callback";

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackListenerService.class);
    private static final Pageable ALL_PAGES = new PageRequest(0, 99999);

    private final ObjectMapper mapper = new ObjectMapper();

    private final TestRepository testRepository;
    private final LockerService lockerService;

    @Autowired
    public CallbackListenerService(TestRepository testRepository, LockerService lockerService) {
        this.testRepository = testRepository;
        this.lockerService = lockerService;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter() {
        return new MessageListenerAdapter((MessageListener) (message, bytes) ->
                callback(new String(message.getBody(), Charset.defaultCharset())));
    }

    @SuppressWarnings("ConstantConditions")
    private void callback(String testStr) {
        try {
            Test testFromLoader = mapper.readValue(testStr, Test.class);

            boolean hasLock = lockerService.waitLockDb();
            if (!hasLock) {
                LOGGER.error("Wait lock DB timeout. Ignoring callback.");
                return;
            }

            Loader loader = testFromLoader.getLoaders().stream().findAny().get();
            Page<Test> pageTestPersisted = testRepository.findByNameAndProject(testFromLoader.getName(), testFromLoader.getProject(), ALL_PAGES);
            Test test = pageTestPersisted != null && pageTestPersisted.getTotalElements() > 0 ? pageTestPersisted.iterator().next() : null;

            if (test == null) {
                LOGGER.error("Test {}.{} NOT FOUND", testFromLoader.getProject(), testFromLoader.getName());
                return;
            }

            Loader loaderFromDb = test.getLoaders().stream().filter(l -> l.equals(loader)).findAny().get();
            if (loaderFromDb.getStatus() == loader.getStatus() && loaderFromDb.getStatusDetailed().equals(loader.getStatusDetailed())) {
                LOGGER.warn("Test {}.{} status: {} (from loader {} [{}])", test.getProject(), test.getName(), test.getStatus().toString(), loader.getName(), loader.getStatus());
                lockerService.releaseLockDb();
                return;
            }

            test.getLoaders().forEach(l -> {
                if (l.getStatus() == Loader.Status.IDLE) l.setStatus(Loader.Status.RUNNING);
                if (l.getName().equals(loader.getName())) {
                    l.setStatus(loader.getStatus());
                    l.setStatusDetailed(loader.getStatusDetailed());
                }
            });
            final Set<Loader> testLoaders = test.getLoaders();
            List<Loader.Status> statuses = testLoaders.stream().map(Loader::getStatus).distinct().collect(Collectors.toList());
            Test.Status status = Test.Status.RUNNING;
            if (statuses.contains(Loader.Status.ERROR)) {
                status = Test.Status.ERROR;
            } else if (statuses.size() == 1) {
                String statusStr = statuses.get(0).toString();
                if (!Loader.Status.IDLE.toString().equals(statusStr)) {
                    status = Enum.valueOf(Test.Status.class, statusStr);
                }
            }
            test.setStatus(status);
            testRepository.save(test);
            LOGGER.info("Test {}.{} status: {} (from loader {} [{}])", test.getProject(), test.getName(), test.getStatus().toString(), loader.getName(), loader.getStatus());

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            lockerService.releaseLockDb();
        }
    }

}
