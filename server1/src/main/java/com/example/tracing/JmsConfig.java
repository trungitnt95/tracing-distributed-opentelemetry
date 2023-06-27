package com.example.tracing;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
//@ComponentScan(basePackages = "com.ngockhuong")
public class JmsConfig {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String BROKER_USERNAME = "admin";
    private static final String BROKER_PASSWORD = "admin";

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(BROKER_URL);
        connectionFactory.setUserName(BROKER_USERNAME);
        connectionFactory.setPassword(BROKER_PASSWORD);

        return connectionFactory;
    }
    @Bean
    public SerLogTraceInfoProvider traceInfoProvider() {
        return new SerLogDefaultTraceInfoProviderImpl();
    }
    @Bean
    public JmsTemplate jmsTemplate(Tracer tracer) {
        JmsTemplate template = new TraceJmsTemplate(tracer);
        template.setConnectionFactory(connectionFactory());

        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency("1-1");
        // true: using jms topic, false: using jms queue
        factory.setPubSubDomain(false);

        return factory;
    }
}