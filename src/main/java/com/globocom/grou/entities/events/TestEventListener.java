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
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestEventListener extends AbstractMongoEventListener<Test> {

    private static final String TEST_QUEUE = "grou:test_queue";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final JmsTemplate template;
    private final TestRepository testRepository;

    @Autowired
    public TestEventListener(JmsTemplate template, TestRepository testRepository) {
        this.template = template;
        this.testRepository = testRepository;
    }

    @Override
    public void onAfterSave(AfterSaveEvent<Test> event) {
        Test test = event.getSource();
        if (test.getStatus() == Test.Status.SCHEDULED) {
            try {
                test.setStatus(Test.Status.ENQUEUED);
                testRepository.save(test);
                template.convertAndSend(TEST_QUEUE, mapper.writeValueAsString(test));
                LOGGER.info("Test {} sent to queue {}", test.getName(), TEST_QUEUE);
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
