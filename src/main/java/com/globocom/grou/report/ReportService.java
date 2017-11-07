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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.globocom.grou.SystemEnv;
import com.globocom.grou.entities.Loader;
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
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
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

    private final TSClient tsClient = TSClient.Type.valueOf(SystemEnv.TS_TYPE.getValue()).INSTANCE;

    private final ObjectMapper mapper = new ObjectMapper()
                                        .registerModule(new SimpleModule().addSerializer(Double.class, new DoubleSerializer()))
                                        .enable(SerializationFeature.INDENT_OUTPUT);

    private final JavaMailSender emailSender;
    private final TestRepository testRepository;
    private final TemplateEngine templateEngine;

    @Autowired
    public ReportService(JavaMailSender emailSender, TestRepository testRepository, TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.testRepository = testRepository;
        this.templateEngine = templateEngine;
    }

    public void send(Test test) throws Exception {
        final AtomicReference<List<Throwable>> exceptions = new AtomicReference<>(new ArrayList<>());
        final Map<String, Double> report = getReport(test);
        final HashMap<String, Double> reportSanitized = sanitizeKeyName(report);
        test.setResult(reportSanitized);
        testRepository.save(test);
        test.getNotify().forEach(notify -> {
            try {
                if (VALID_EMAIL_ADDRESS_REGEX.matcher(notify).matches()) {
                    notifyByMail(test, notify.replaceAll("^mailto:[/]{0,2}", ""), report);
                } else if (VALID_HTTP_ADDRESS_REGEX.matcher(notify).matches()) {
                    notifyByHttp(test, notify);
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

    private HashMap<String, Double> sanitizeKeyName(Map<String, Double> report) {
        HashMap<String, Double> reportSanitized = new HashMap<>();
        report.forEach((key, value) -> reportSanitized.put(
                key.replaceAll("[.\\s\\\\()%/:\\-]", "_")
                   .replaceAll("_{2,}", "_")
                   .replaceAll("_$", "").toLowerCase(), value));
        return reportSanitized;
    }

    private Map<String, Double> getReport(Test test) throws Exception {
        final Map<String, Double> result = tsClient.makeReport(test);
        if (result != null && !result.isEmpty()) return result;
        String errorMsg = "INTERNAL ERROR (See GROU Log)";
        LOGGER.error(errorMsg);
        throw new RuntimeException(errorMsg);
    }

    private void notifyByHttp(Test test, String url) throws IOException {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(test.getResult())));
            Arrays.stream(HEADERS).forEach(httpPost::setHeader);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            LOGGER.info("Test " + test.getProject() + "." + test.getName() + ": sent notification to " + url + " [response status=" + response.getStatusLine().getStatusCode() + "]");
        }
    }

    private void notifyByMail(Test test, String email, Map<String, Double> result) throws Exception {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setTo(email);
            messageHelper.setFrom(MAIL_FROM);
            messageHelper.setSubject(getSubject(test));
            Context context = new Context();
            context.setVariable("project", test.getProject());
            context.setVariable("name", test.getName());
            HashMap<String, Object> testContext = new HashMap<>();
            testContext.put("dashboard", test.getDashboard());
            testContext.put("loaders", test.getLoaders().stream().map(Loader::getName).collect(Collectors.toSet()));
            testContext.put("properties", test.getProperties());
            testContext.put("id", test.getId());
            testContext.put("created", test.getCreatedDate().toString());
            testContext.put("lastModified", test.getLastModifiedDate().toString());
            testContext.put("durationTimeMillis", test.getDurationTimeMillis());
            context.setVariable("testContext", mapper.writeValueAsString(testContext).split("\\R"));
            Set<String> tags = test.getTags();
            context.setVariable("tags", tags);
            context.setVariable("metrics", new TreeMap<>(result));
            String content = templateEngine.process("reportEmail", context);
            messageHelper.setText(content, true);
        };
        try {
            emailSender.send(messagePreparator);
            LOGGER.info("Test " + test.getProject() + "." + test.getName() + ": sent notification to email " + email);
        } catch (MailException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private String getSubject(Test test) {
        return "Test " + test.getProject() + "." + test.getName() + " finished with status " + test.getStatus();
    }

    public static class DoubleSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double aDouble, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeNumber(BigDecimal.valueOf(aDouble).toPlainString());
        }
    }
}
