package com.globocom.grou.entities.events.services;

import com.globocom.grou.entities.Loader;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.events.CallbackEvent;
import com.globocom.grou.entities.repositories.LoaderRepository;
import com.globocom.grou.entities.repositories.TestRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
public class CallbackListenerService {

    private static final String CALLBACK_QUEUE = "grou:test_callback";

    private final Log log = LogFactory.getLog(this.getClass());

    private final TestRepository testRepository;
    private final LoaderRepository loaderRepository;

    @Autowired
    public CallbackListenerService(TestRepository testRepository, LoaderRepository loaderRepository) {
        this.testRepository = testRepository;
        this.loaderRepository = loaderRepository;
    }

    @JmsListener(destination = CALLBACK_QUEUE)
    public void callback(CallbackEvent event) {
        Test test = event.getTest();
        Loader loader = event.getLoader();
        testRepository.save(test);
        loaderRepository.save(loader);

        log.info("Test " + test.getName() + " status: " + test.getStatus().toString() + " (from loader " + loader.getUrl() + ")");
    }
}
