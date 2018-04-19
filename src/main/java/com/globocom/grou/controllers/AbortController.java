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

import com.globocom.grou.entities.Loader;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.services.LoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
public class AbortController {

    private final StringRedisTemplate redisTemplate;
    private final LoaderService loaderService;

    @Autowired
    public AbortController(StringRedisTemplate redisTemplate, LoaderService loaderService) {
        this.redisTemplate = redisTemplate;
        this.loaderService = loaderService;
    }

    @PostMapping(value = "/abort", consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> receiveAbort(@RequestBody Test test) {
        String testName = test.getName();
        String projectName = test.getProject();
        final List<Loader> loaders = loaderService.loaders().stream()
                .flatMap(groupLoader -> groupLoader.getLoaders().stream())
                .collect(Collectors.toList());
        if (loaders.isEmpty()) {
            registerAbortKey(testName, projectName, "UNDEF");
        } else {
            loaders.stream().map(Loader::getName).forEach(loaderId -> registerAbortKey(testName, projectName, loaderId));
        }
        return ResponseEntity.accepted().build();
    }

    private void registerAbortKey(String testName, String projectName, String loaderId) {
        String abortKey = "ABORT:" + projectName + "." + testName + "#" + loaderId;
        redisTemplate.opsForValue().set(abortKey, UUID.randomUUID().toString(), 60000, TimeUnit.MILLISECONDS);
    }
}
