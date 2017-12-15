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

import com.globocom.grou.SystemEnv;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/token/{project:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Token> token(@PathVariable String project, @RequestHeader("Authorization") String authorization) {
        if (authorization.startsWith("Basic")) {
            Pair<String, String> credentials = getCredentials(authorization);
            String username = credentials.getFirst();
            String password = credentials.getSecond();

            try {
                final String tokenId;
                if (Boolean.parseBoolean(SystemEnv.DISABLE_AUTH.getValue())) {
                    tokenId = "AUTH_DISABLED";
                } else {
                    tokenId = OSFactory.builderV3()
                            .endpoint(KEYSTONE_URL.getValue())
                            .credentials(username, password, Identifier.byName(KEYSTONE_DOMAIN_CONTEXT.getValue()))
                            .scopeToProject(Identifier.byName(project), Identifier.byName(KEYSTONE_DOMAIN_CONTEXT.getValue()))
                            .authenticate().getToken().getId();
                }

                final HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set("x-auth-token", tokenId);

                boolean isAdmin = project.equals(SystemEnv.PROJECT_ADMIN.getValue());
                return new ResponseEntity<>(new Token(tokenId, isAdmin), responseHeaders, HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return new ResponseEntity<>((Token) null, HttpStatus.UNAUTHORIZED);
            }
        }
        return new ResponseEntity<>((Token) null, HttpStatus.PRECONDITION_REQUIRED);
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

    @SuppressWarnings("WeakerAccess")
    public static class Token {
        public final String token;
        public final boolean isAdmin;

        public Token(String token, boolean isAdmin) {
            this.token = token;
            this.isAdmin = isAdmin;
        }
    }
}
