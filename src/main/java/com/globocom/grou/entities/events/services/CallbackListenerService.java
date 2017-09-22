package com.globocom.grou.entities.events.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CallbackListenerService {

    private static final String CALLBACK_QUEUE = "grou:test_callback";

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackListenerService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final TestRepository testRepository;

    @Autowired
    public CallbackListenerService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    @JmsListener(destination = CALLBACK_QUEUE, concurrency = "1-1")
    public void callback(String testStr) throws IOException {
        Test test = mapper.readValue(testStr, Test.class);
        testRepository.save(test);
        LOGGER.info("Test {} status: {} (from loader {})", test.getName(), test.getStatus().toString(), test.getLoader());
    }

}
