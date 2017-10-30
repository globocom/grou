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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        final String report = getReport(test);
        test.getNotify().forEach(notify -> {
            try {
                if (VALID_EMAIL_ADDRESS_REGEX.matcher(notify).matches()) {
                    notifyByMail(test, notify.replaceAll("^mailto:[/]{0,2}", ""));
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

    private String getReport(Test test) {
        final Map<String, Double> result = tsClient.makeReport(test);
        try {
            test.setResult(new HashMap<>(result));
            testRepository.save(test);
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return "{ \"error\": \"INTERNAL ERROR (See GROU Log)\"";
        }
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

    private void notifyByMail(Test test, String email) throws Exception {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setTo(email);
            messageHelper.setFrom(MAIL_FROM);
            messageHelper.setSubject(getSubject(test));
            Context context = new Context();
            context.setVariable("project", test.getProject());
            context.setVariable("name", test.getName());
            String tags = test.getTags().stream().collect(Collectors.joining(","));
            context.setVariable("tags", (tags.isEmpty() ? "UNDEF" : tags));
            context.setVariable("metrics", new TreeMap<>(test.getResult()));
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
            BigDecimal d = new BigDecimal(aDouble);
            jsonGenerator.writeNumber(d.toPlainString());
        }
    }
}
