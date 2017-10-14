package com.globocom.grou.schedule;

import com.globocom.grou.SystemEnv;
import com.globocom.grou.entities.Loader;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.StreamSupport;

@Service
public class SchedulerService {

    public static final int CONSUMER_PAUSE = Integer.parseInt(SystemEnv.CONSUMER_PAUSE.getValue());
    private final TestRepository testRepository;
    private Pageable pageable = new PageRequest(0, 99999);

    @Autowired
    public SchedulerService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void fixStatus() {
        Instant instante = Instant.now().minus((Integer.parseInt(SystemEnv.MAX_RETRY.getValue()) * CONSUMER_PAUSE), ChronoUnit.SECONDS);
        Test.Status status = Test.Status.ENQUEUED;
        Page<Test> tests = testRepository.findByLastModifiedByBeforeAndStatus(instante, status, pageable);
        StreamSupport.stream(tests.spliterator(), false).forEach(t -> {
            Loader loaderUndef = new Loader();
            loaderUndef.setName("UNDEF");
            loaderUndef.setStatus(Loader.Status.ERROR);
            loaderUndef.setStatusDetailed("Forgotten in the queue");
            t.setStatus(Test.Status.ERROR);
            testRepository.save(t);
        });
    }
}
