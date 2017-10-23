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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
            Test test = getTestFromDB(testFromLoader);
            if (testNotExist(testFromLoader, test)) return;

            Test.Status status;
            final Set<Loader> testLoaders = test.getLoaders();
            if (!test.getLoaders().isEmpty()) {
                if (hasNoChanges(loader, test, testLoaders)) return;
                syncStatus(loader, testLoaders);
                status = checkConsensusOrError(testLoaders);
            } else {
                test.setLoaders(testFromLoader.getLoaders());
                status = loader.getStatusDetailed().equals(Test.Status.ABORTED.toString()) ? Test.Status.ABORTED : Test.Status.ERROR;
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

    private boolean testNotExist(Test testFromLoader, Test test) {
        if (test == null) {
            LOGGER.error("Test {}.{} NOT FOUND", testFromLoader.getProject(), testFromLoader.getName());
            return true;
        }
        return false;
    }

    private boolean hasNoChanges(Loader loader, Test test, Set<Loader> testLoaders) {
        Optional<Loader> loaderFromDb = testLoaders.stream().filter(l -> l.equals(loader)).findAny();
        if (loaderFromDb.isPresent() && loaderFromDb.get().getStatus() == loader.getStatus() && loaderFromDb.get().getStatusDetailed().equals(loader.getStatusDetailed())) {
            LOGGER.warn("Test {}.{} status: {} (from loader {} [{}])", test.getProject(), test.getName(), test.getStatus().toString(), loader.getName(), loader.getStatus());
            lockerService.releaseLockDb();
            return true;
        }
        return false;
    }

    private Test getTestFromDB(Test testFromLoader) {
        Page<Test> pageTestPersisted = testRepository.findByNameAndProject(testFromLoader.getName(), testFromLoader.getProject(), ALL_PAGES);
        return pageTestPersisted != null && pageTestPersisted.getTotalElements() > 0 ? pageTestPersisted.iterator().next() : null;
    }

    @SuppressWarnings("ConstantConditions")
    private Test.Status checkConsensusOrError(Set<Loader> testLoaders) {
        Set<Loader.Status> statuses = new HashSet<>();
        Set<String> statusDetailed = new HashSet<>();
        Test.Status status = Test.Status.RUNNING;

        testLoaders.forEach(loader -> {
            statuses.add(loader.getStatus());
            statusDetailed.add(loader.getStatusDetailed());
        });
        if (statuses.contains(Loader.Status.ERROR)) {
            status = statusDetailed.contains(Test.Status.ABORTED.toString()) ? Test.Status.ABORTED : Test.Status.ERROR;
        } else if (thereIsConsensus(statuses)) {
            String statusStr = statuses.stream().findAny().get().toString();
            if (!Loader.Status.IDLE.toString().equals(statusStr)) {
                status = Enum.valueOf(Test.Status.class, statusStr);
            }
        }
        return status;
    }

    private void syncStatus(Loader loader, Set<Loader> testLoaders) {
        testLoaders.forEach(l -> {
            if (l.getStatus() == Loader.Status.IDLE) l.setStatus(Loader.Status.RUNNING);
            if (l.getName().equals(loader.getName())) {
                l.setStatus(loader.getStatus());
                l.setStatusDetailed(loader.getStatusDetailed());
            }
        });
    }

    private boolean thereIsConsensus(Set<Loader.Status> statuses) {
        return statuses.size() == 1;
    }

}
