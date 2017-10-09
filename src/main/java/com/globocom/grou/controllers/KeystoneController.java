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

import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.nio.charset.Charset;
import java.util.Base64;

import static com.globocom.grou.SystemEnv.KEYSTONE_DOMAIN_CONTEXT;
import static com.globocom.grou.SystemEnv.KEYSTONE_URL;

@Controller
public class KeystoneController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoneController.class);

    @GetMapping("/token/{project:.+}")
    public ResponseEntity<String> token(@PathVariable String project, @RequestHeader("Authorization") String auth) {
        if (auth.startsWith("Basic")) {
            Pair<String, String> credentials = getCredentials(auth);
            String username = credentials.getFirst();
            String password = credentials.getSecond();

            try {
                String body = OSFactory.builderV3()
                                    .endpoint(KEYSTONE_URL.getValue())
                                    .credentials(username, password, Identifier.byName(KEYSTONE_DOMAIN_CONTEXT.getValue()))
                                    .scopeToProject(Identifier.byName(project), Identifier.byName(KEYSTONE_DOMAIN_CONTEXT.getValue()))
                                    .authenticate().getToken().getId();
                return ResponseEntity.ok(body);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return new ResponseEntity<>("", HttpStatus.UNAUTHORIZED);
            }
        }
        return new ResponseEntity<>("", HttpStatus.PRECONDITION_REQUIRED);
    }

    private Pair<String, String> getCredentials(String auth) {
        String base64Credentials = auth.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.defaultCharset());
        final String[] values = credentials.split(":", 2);
        if (values.length > 1) {
            return Pair.of(values[0], values[1]);
        }
        return Pair.of("", "");
    }
}
