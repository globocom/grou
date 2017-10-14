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

package com.globocom.grou.entities.events.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LockerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockerService.class);

    private static final String GROU_LOCK_JMS = "grou:lock:jms";
    private static final String GROU_LOCK_DB = "grou:lock:db";

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public LockerService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public synchronized boolean lockJms() throws Exception {
        boolean locked = redisTemplate.opsForValue().setIfAbsent(GROU_LOCK_JMS, UUID.randomUUID().toString());
        if (locked) redisTemplate.expire(GROU_LOCK_JMS, 10, TimeUnit.SECONDS);
        return locked;
    }

    public void releaseLockJms() {
        redisTemplate.delete(GROU_LOCK_JMS);
    }

    public boolean waitLockDb() throws Exception {
        long start = System.currentTimeMillis();
        boolean locked;
        while (!(locked = redisTemplate.opsForValue().setIfAbsent(GROU_LOCK_DB, UUID.randomUUID().toString()))) {
            LOGGER.warn("DB lock detected. Waiting release db lock");
            Thread.sleep(1000);
            if (start + 10000 < System.currentTimeMillis()) break;
        }
        if (locked) redisTemplate.expire(GROU_LOCK_DB, 10, TimeUnit.SECONDS);
        return locked;
    }

    public void releaseLockDb() {
        redisTemplate.delete(GROU_LOCK_DB);
    }
}
