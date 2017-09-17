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

import org.apache.activemq.artemis.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.Optional;

import static org.springframework.jms.core.JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT;

@Configuration
@EnableJms
public class JmsConfiguration {

    private static final long DEFAULT_JMS_TIMEOUT  = Long.parseLong(Optional.ofNullable(System.getenv("JMS_TIMEOUT")).orElse("10000"));

    private static final String BROKER_CONN = Optional.ofNullable(System.getenv("BROKER_CONN")).orElse("tcp://localhost:61616?blockOnDurableSend=false&consumerWindowSize=0&protocols=Core");
    private static final String BROKER_USER = Optional.ofNullable(System.getenv("BROKER_USER")).orElse("guest");
    private static final String BROKER_PASS = Optional.ofNullable(System.getenv("BROKER_PASS")).orElse("guest");
    private static final boolean  BROKER_HA = Boolean.parseBoolean(Optional.ofNullable(System.getenv("BROKER_HA")).orElse("false"));

    @Bean(name="jmsConnectionFactory")
    public CachingConnectionFactory cachingConnectionFactory() throws JMSException {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_CONN);
        connectionFactory.setUser(BROKER_USER);
        connectionFactory.setPassword(BROKER_PASS);
        if (BROKER_HA) {
            connectionFactory.setConnectionLoadBalancingPolicyClassName(RoundRobinConnectionLoadBalancingPolicy.class.getName());
        }
        cachingConnectionFactory.setTargetConnectionFactory(connectionFactory);
        cachingConnectionFactory.setSessionCacheSize(100);
        cachingConnectionFactory.setCacheConsumers(true);
        return cachingConnectionFactory;
    }

    @Bean(name = "jmsTemplate")
    public JmsTemplate jmsTemplate(@Value("#{jmsConnectionFactory}") ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setDeliveryPersistent(false);
        jmsTemplate.setReceiveTimeout(RECEIVE_TIMEOUT_NO_WAIT);
        jmsTemplate.setTimeToLive(DEFAULT_JMS_TIMEOUT);
        return jmsTemplate;
    }

}
