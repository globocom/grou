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

import com.globocom.grou.entities.Test;
import com.globocom.grou.report.ts.TSClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);

    private final TSClient tsClient = TSClient.Type.valueOf("OPENTSDB").INSTANCE;

    private final JavaMailSender emailSender;

    @Autowired
    public ReportService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void send(Test test) throws Exception {
        String notify = (String) test.getProperties().get("notify");
        if (notify != null) {
            String report = getReport(test);
            if (notify.matches("^mailto:[\\w.+-]+@[\\w.]+$")) {
                notifyByMail(test, notify, report);
            } else if (notify.matches("^http[s]?://.+$")) {
                notifyByHttp(test, notify, report);
            } else {
                throw new UnsupportedOperationException("notify destination unsupported: " + notify);
            }
        }
    }

    private String getReport(Test test) {
        return tsClient.makeReport(test);
    }

    private void notifyByHttp(Test test, String url, String report) throws IOException {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(report));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            LOGGER.info("Test " + test.getProject() + "." + test.getName() + ": sent notification to " + url + " with status " + response.getStatusLine().getStatusCode());
        }
    }

    private void notifyByMail(Test test, String email, String report) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("GROU Report - Test " + test.getProject() + "." + test.getName());
        message.setText(report);
        emailSender.send(message);
    }
}
