package com.globocom.grou.entities.events.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globocom.grou.entities.Loader;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import static com.globocom.grou.entities.Test.Status.ERROR;
import static com.globocom.grou.entities.Test.Status.RUNNING;

@Service
public class CallbackListenerService {

    public static final String CALLBACK_QUEUE = "grou:test_callback";

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackListenerService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final TestRepository testRepository;

    @Autowired
    public CallbackListenerService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter() {
        return new MessageListenerAdapter((MessageListener) (message, bytes) -> {
            try {
                byte[] body = message.getBody();
                callback(new String(body, Charset.defaultCharset()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    private void callback(String testStr) throws IOException {
        Test test = mapper.readValue(testStr, Test.class);
        List<Test.Status> statuses = test.getLoaders().stream().map(Loader::getStatus).distinct().collect(Collectors.toList());
        Test.Status status = RUNNING;
        if (statuses.contains(ERROR)) {
            status = ERROR;
        } else if (statuses.size() == 1) {
            status = statuses.get(0);
        }
        test.setStatus(status);
        testRepository.save(test);
        String loaders = test.getLoaders().stream().map(Loader::getName).collect(Collectors.joining(","));
        LOGGER.info("Test {} status: {} (from loaders {})", test.getProject() + "." + test.getName(), test.getStatus().toString(), loaders);
    }

}
