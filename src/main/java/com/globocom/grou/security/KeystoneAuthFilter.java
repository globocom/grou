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

package com.globocom.grou.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globocom.grou.entities.Project;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.ProjectRepository;
import com.globocom.grou.http.CachedHttpServletRequest;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class KeystoneAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_PROBLEM_ERRORMSG = "Auth problem. Check token or project scope";
    public static final String AUTH_TOKEN_HEADER     = "X-Auth-Token";
    public static final String PROJECT_SCOPE_HEADER  = "X-Project";

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = new ObjectMapper();

    private final ProjectRepository projectRepository;

    public KeystoneAuthFilter(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean isTestRequest = ("/" + Test.class.getSimpleName().toLowerCase() + "s").equals(request.getRequestURI());
        boolean isPostMethod = HttpMethod.POST.toString().equals(request.getMethod());

        String token = request.getHeader(AUTH_TOKEN_HEADER);
        if (token == null) {
            throw new SecurityException(AUTH_PROBLEM_ERRORMSG);
        }
        if (isTestRequest && isPostMethod) {
            request = new CachedHttpServletRequest(request);
            JsonNode testJson = getJsonFromBody(request);
            String projectName = getProjectFromBody(testJson);

            auth(token, projectName);

            String bodyRewritten = cloneTestFromJsonBody(testJson, projectName);
            writeRequestBody(request, bodyRewritten);

        } else {
            String projectName = request.getHeader(PROJECT_SCOPE_HEADER);
            auth(token, projectName);
        }
        filterChain.doFilter(request, response);
    }

    private void writeRequestBody(HttpServletRequest request, String testClonedWithProject) {
        ByteArrayOutputStream newBodyOS = new ByteArrayOutputStream(testClonedWithProject.length());
        if (log.isDebugEnabled()) log.debug("Rewrite body to " + testClonedWithProject);
        newBodyOS.write(testClonedWithProject.getBytes(), 0, testClonedWithProject.length());
        ((CachedHttpServletRequest)request).setCachedBytes(newBodyOS);
    }

    private String cloneTestFromJsonBody(JsonNode jsonBody, String projectName) throws JsonProcessingException {
        Project newProject;
        if ((newProject = projectRepository.findByName(projectName)) == null) {
            projectRepository.save(new Project(projectName));
            newProject = projectRepository.findByName(projectName);
        }
        ((ObjectNode)jsonBody).replace(Project.class.getSimpleName().toLowerCase(), mapper.valueToTree(newProject));
        return jsonBody.toString();
    }

    private JsonNode getJsonFromBody(HttpServletRequest request) throws IOException {
        String bodyStr = CharStreams.toString(new InputStreamReader(request.getInputStream(), Charset.defaultCharset()));
        return mapper.readTree(bodyStr);
    }

    private String getProjectFromBody(JsonNode json) throws IOException {
        return json.get(Project.class.getSimpleName().toLowerCase()).get("name").asText();
    }

    private void auth(String xAuthToken, String projectName) throws SecurityException {
        Assert.notNull(projectName, "Project is NULL");

        log.info("Using project: " + projectName);

        Authentication auth = new KeystoneAuthenticationToken(xAuthToken, projectName);
        SecurityContextHolder.getContext().setAuthentication(auth);
        if (auth.getPrincipal() == null) throw new SecurityException(AUTH_PROBLEM_ERRORMSG);
    }
}
