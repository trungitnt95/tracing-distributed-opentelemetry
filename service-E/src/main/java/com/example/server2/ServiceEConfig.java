package com.example.server2;

import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Configuration
public class ServiceEConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
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
        return new CustomBaggageTaggingSpanProcessor(List.of("bg-field2", "bg-field3"));
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
}
