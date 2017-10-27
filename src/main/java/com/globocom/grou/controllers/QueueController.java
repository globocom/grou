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

package com.globocom.grou.controllers;

import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueueController {

    private final TestRepository testRepository;

    @Autowired
    public QueueController(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    @GetMapping("/queue")
    public Page<Test> report(@RequestParam(value = "project", required = false) String project, Pageable pageable) {
        if (project == null) return testRepository.findByStatus(Test.Status.ENQUEUED, pageable);
        return testRepository.findByProjectAndStatus(project, Test.Status.ENQUEUED, pageable);
    }
}
