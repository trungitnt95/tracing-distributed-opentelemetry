package com.example.tracing;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.otel.bridge.BaggageTaggingSpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Configuration
@EnableJms
public class ServiceBConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBConfig.class);
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

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Test code to print out all headers - you don't need this code
//                .additionalInterceptors((request, body, execution) -> {
//                    String b3 = request.getHeaders().get("b3").get(0);
//                    request.getHeaders().put("traceparent", List.of("00-" + b3.substring(0, b3.length()-2) +"-01"));
//                    request.getHeaders();
//                    request.getHeaders().forEach((s, strings) -> LOGGER.info("HEADER [{}] VALUE {}", s, strings));
//                    return execution.execute(request, body);
//                })
                .build();
    }


    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
//                .filter((request, next) -> {
//                    ClientRequest modifiedRequest = ClientRequest.from(request)
//                            .headers(headers -> {
//                                String b3 = headers.get("b3").get(0);
//                                headers.put("traceparent", List.of("00-" + b3.substring(0, b3.length()-2) +"-01"));
//                            })
//                            .build();
//                    return next.exchange(modifiedRequest);
//                })
                .build();
    }

    /**
     * BUG in OtelPropagationConfiguration#BaggageTaggingConfiguration.
     * It cannot set tag fields into span from application properties.
     * Adding manually instead.
     * // TODO: need to check when migrate to Micrometer + SpringBoot 3.x
     * @return
     */
    @Bean
    public BaggageTaggingSpanProcessor baggageTaggingSpanProcessor() {
        return new CustomBaggageTaggingSpanProcessor(List.of("bg-field2", "bg-field3", "x-tracing-flow-code"));
    }

    class CustomBaggageTaggingSpanProcessor extends BaggageTaggingSpanProcessor {

        private final Map<String, AttributeKey<String>> tagsToApply;

        public CustomBaggageTaggingSpanProcessor(List<String> tagsToApply) {
            super(tagsToApply);
            this.tagsToApply = tagsToApply.stream().map(AttributeKey::stringKey)
                    .collect(toMap(AttributeKey::getKey, key -> key));
        }

        @Override
        public void onStart(Context context, ReadWriteSpan readWriteSpan) {
            Baggage baggage = Baggage.fromContext(context);

            baggage.forEach((key, baggageEntry) -> {
                AttributeKey<String> attributeKey = tagsToApply.get(key);
                if (attributeKey != null && baggageEntry.getValue()!= null && !"".equals(baggageEntry.getValue())) {
                    readWriteSpan.setAttribute(attributeKey, baggageEntry.getValue());
                }
            });
            SpanData spanData = readWriteSpan.toSpanData();
            readWriteSpan.setAttribute("x-trace-id", spanData.getTraceId());
            readWriteSpan.setAttribute("x-span-id", spanData.getSpanId());
            if (!"0000000000000000".equals(readWriteSpan.getParentSpanContext().getSpanId())) {
                readWriteSpan.setAttribute("x-parent-span-id", spanData.getParentSpanId());
            }

            String httpPath = readWriteSpan.getAttribute(AttributeKey.stringKey("http.path"));
            if (StringUtils.isNotBlank(httpPath)) {
                readWriteSpan.updateName(readWriteSpan.getName() + StringUtils.SPACE + httpPath);
            }
        }
    }

    @Bean
    public JmsListenerAspect jmsListenerAspect() {
        return new JmsListenerAspect();
    }
}
