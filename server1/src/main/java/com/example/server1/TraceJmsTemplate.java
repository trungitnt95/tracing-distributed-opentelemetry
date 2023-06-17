package com.example.server1;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import java.util.Map;
import java.util.Optional;

public class TraceJmsTemplate extends JmsTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceJmsTemplate.class);

    private final Tracer tracer;

    public TraceJmsTemplate(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doSend(MessageProducer producer, Message message) throws JMSException {
        Span.Builder spanBuilder = tracer.spanBuilder();
        spanBuilder.name("JMS - SEND " + producer.getDestination().toString());
        Optional.ofNullable(tracer.currentTraceContext())
                .map(CurrentTraceContext::context)
                .ifPresent(spanBuilder::setParent);

        Span jmsSpan = spanBuilder.start();
        pushTraceInfo(message, jmsSpan);
        try {
            super.doSend(producer, message);
        } catch (JMSException exception) {
            jmsSpan.error(exception);
            throw exception;
        } finally {
            jmsSpan.end();
        }
    }
    @SuppressWarnings("unchecked")
    private void pushTraceInfo(Message message, Span jmsSpan) throws JMSException {
        Map<String, String> currentBaggage = tracer.getAllBaggage();
        for (Map.Entry<String, String> entry : currentBaggage.entrySet()) {
            message.setStringProperty(entry.getKey(), entry.getValue());
        }

        message.setStringProperty("x-trace-id", jmsSpan.context().traceId());
        message.setStringProperty("x-span-id", jmsSpan.context().spanId());
        if (StringUtils.isNotBlank(jmsSpan.context().parentId())) {
            message.setStringProperty("x-parent-span-id", jmsSpan.context().parentId());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" === JMS Message Properties after enrich === ");
            java.util.Enumeration<String> en = message.getPropertyNames();
            while (en.hasMoreElements()) {
                String propName = en.nextElement();
                LOGGER.debug(" === Property: {}:{} === ", propName, message.getStringProperty(propName));
            }
        }
    }
}

