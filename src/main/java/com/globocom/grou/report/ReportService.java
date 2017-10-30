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

package com.globocom.grou.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.globocom.grou.SystemEnv;
import com.globocom.grou.entities.Test;
import com.globocom.grou.entities.repositories.TestRepository;
import com.globocom.grou.report.ts.TSClient;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger   LOGGER = LoggerFactory.getLogger(ReportService.class);

    private static final Pattern  VALID_EMAIL_ADDRESS_REGEX =
                                                Pattern.compile("^mailto:[/]{0,2}[A-Z0-9._%+-]+@[A-Z0-9.-]+$",
                                                Pattern.CASE_INSENSITIVE);

    private static final Pattern  VALID_HTTP_ADDRESS_REGEX =
                                                Pattern.compile("^http[s]?://.+$",
                                                Pattern.CASE_INSENSITIVE);

    private static final Header[] HEADERS = { new BasicHeader("Accept", "application/json"),
                                              new BasicHeader("Content-type", "application/json") };

    private static final String   MAIL_FROM = SystemEnv.REPORT_MAIL_FROM.getValue();

    private final TSClient tsClient = TSClient.Type.valueOf("OPENTSDB").INSTANCE;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final JavaMailSender emailSender;
    private final TestRepository testRepository;

    @Autowired
    public ReportService(JavaMailSender emailSender, TestRepository testRepository) {
        this.emailSender = emailSender;
        this.testRepository = testRepository;
    }

    public void send(Test test) throws Exception {
        final AtomicReference<List<Throwable>> exceptions = new AtomicReference<>(new ArrayList<>());
        final String report = getReport(test);
        test.getNotify().forEach(notify -> {
            try {
                if (VALID_EMAIL_ADDRESS_REGEX.matcher(notify).matches()) {
                    notifyByMail(test, notify.replaceAll("^mailto:[/]{0,2}", ""), report);
                } else if (VALID_HTTP_ADDRESS_REGEX.matcher(notify).matches()) {
                    notifyByHttp(test, notify, report);
                } else {
                    throw new UnsupportedOperationException("notify destination unsupported: " + notify);
                }
            } catch (Exception e) {
                exceptions.get().add(e);
            }
        });
        String exceptionsStr = exceptions.get().stream().map(Throwable::getMessage).collect(Collectors.joining(" "));
        if (!exceptionsStr.isEmpty()) {
            throw new IllegalStateException(exceptionsStr);
        }
    }

    @SuppressWarnings("unchecked")
    private String getReport(Test test) {
        ArrayList<HashMap<String, Object>> result = tsClient.makeReport(test);
        if (result != null) {
            try {
                final HashMap<String, Double> mapOfResult = new HashMap<>();
                result.forEach(m ->
                    mapOfResult.put(
                            ((String) m.get("metric")).replaceAll("[.]", "_"),
                            ((Map<String, Double>) m.get("dps")).entrySet().stream()
                                    .mapToDouble(Map.Entry::getValue).average().orElse(-1.0))
                );
                test.setResult(mapOfResult);
                testRepository.save(test);
                return mapper.writeValueAsString(mapOfResult);
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
                return "{ \"error\": \"INTERNAL ERROR (See GROU Log)\"";
            }
        }
        LOGGER.error("Test {}.{}: makeReport return NULL", test.getProject(), test.getName());
        return "{ \"error\": \"INTERNAL ERROR (See GROU Log)\"";
    }

    private void notifyByHttp(Test test, String url, String report) throws IOException {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(report));
            Arrays.stream(HEADERS).forEach(httpPost::setHeader);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            LOGGER.info("Test " + test.getProject() + "." + test.getName() + ": sent notification to " + url + " [response status=" + response.getStatusLine().getStatusCode() + "]");
        }
    }

    private void notifyByMail(Test test, String email, String report) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(MAIL_FROM);
        message.setSubject(getSubject(test));
        message.setText(report);
        emailSender.send(message);
        LOGGER.info("Test " + test.getProject() + "." + test.getName() + ": sent notification to email " + email);
    }

    private String getSubject(Test test) {
        return "Test " + test.getProject() + "." + test.getName() + " finished with status " + test.getStatus();
    }
}
