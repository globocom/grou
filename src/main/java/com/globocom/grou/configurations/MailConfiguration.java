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

package com.globocom.grou.configurations;

import com.globocom.grou.SystemEnv;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Session;
import java.util.Properties;

@Configuration
public class MailConfiguration extends MailSenderAutoConfiguration {

    public MailConfiguration(MailProperties properties, ObjectProvider<Session> session) {
        super(properties, session);
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        String mailHost = SystemEnv.MAIL_HOST.getValue();
        if (!"".equals(mailHost)) {
            mailSender.setHost(mailHost);
            int mailPort = Integer.parseInt(SystemEnv.MAIL_PORT.getValue());
            mailSender.setPort(mailPort);

            Properties props = mailSender.getJavaMailProperties();
            String mailTransport = SystemEnv.MAIL_TRANSPORT.getValue();
            props.put("mail.transport.protocol", mailTransport);
            String mailTls = SystemEnv.MAIL_TLS.getValue();
            props.put("mail.smtp.starttls.enable", mailTls);
            String mailUser = SystemEnv.MAIL_USER.getValue();
            if (!"".equals(mailUser)) {
                props.put("mail.smtp.auth", "true");
                mailSender.setUsername(mailUser);
                String mailPass = SystemEnv.MAIL_PASS.getValue();
                mailSender.setPassword(mailPass);
            }
        }
        return mailSender;
    }

}
