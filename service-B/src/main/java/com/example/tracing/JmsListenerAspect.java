package com.example.tracing;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.BaggageInScope;
import org.springframework.cloud.sleuth.BaggageManager;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

import javax.jms.Destination;
import javax.jms.Message;
import java.util.List;

@Aspect
public class JmsListenerAspect {

    @Autowired
    private Tracer tracer;
    @Autowired
    private BaggageManager baggageManager;

    @Around("@annotation(org.springframework.jms.annotation.JmsListener) && args(message)")
    public Object aroundJmsListener(ProceedingJoinPoint joinPoint, Message message) throws Throwable {
        CurrentTraceContext currentTraceContext = tracer.currentTraceContext();
        if (currentTraceContext == null) {// don't have any dependency of Brave or OTel trace
            return joinPoint.proceed();
        }
        Span parentSpan = null;
        try {
            TraceContext traceContext = tracer.traceContextBuilder()
                    .sampled(true)
                    .traceId(message.getStringProperty("x-trace-id"))
                    .spanId(message.getStringProperty("x-span-id"))
                    .parentId(message.getStringProperty("x-parent-span-id"))
                    .build();

            for (String key : List.of("bg-field1","bg-field2","bg-field3","bg-field4")) {
                String value = message.getStringProperty(key);

                try (BaggageInScope baggageInScope = baggageManager.createBaggage(key)) {
                    baggageInScope.set(traceContext, value);
                }
            }

            currentTraceContext.newScope(traceContext);
            TraceContext context = currentTraceContext.context();
            Destination destination = message.getJMSDestination();
            parentSpan = tracer.spanBuilder().setParent(context).name("JMS-RECEIVE " + destination).start();

            return joinPoint.proceed();
        } catch (Throwable exception) {
            if (parentSpan != null) {
                parentSpan.error(exception);
            }
            throw exception;
        } finally {
            if (parentSpan != null) {
                parentSpan.end();
            }
        }
    }


}
