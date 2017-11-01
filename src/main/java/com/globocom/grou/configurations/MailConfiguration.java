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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.mail.Session;
import java.math.BigDecimal;

@Configuration
public class MailConfiguration extends MailSenderAutoConfiguration {

    public MailConfiguration(MailProperties properties, ObjectProvider<Session> session) {
        super(properties, session);
    }

    @Bean
    public DoubleConverter doubleConverter() {
        return new DoubleConverter();
    }

    public class DoubleConverter {
        public String stringify(Double aDouble) {
            return new BigDecimal(aDouble).toPlainString();
        }
    }
}
