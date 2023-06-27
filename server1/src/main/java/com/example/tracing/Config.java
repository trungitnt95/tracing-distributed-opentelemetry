package com.example.tracing;

import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Configuration
public class Config {

    Logger LOGGER = LoggerFactory.getLogger(Config.class);

    // You must register RestTemplate as a bean!
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Test code to print out all headers - you don't need this code
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().forEach((s, strings) -> LOGGER.info("HEADER [{}] VALUE {}", s, strings));
                    return execution.execute(request, body);
                })
                .build();
    }



//    @Bean
//    public WebClient.Builder webClientBuilder() {
//        return WebClient.builder()
////                .filter((request, next) -> {
////            ClientRequest modifiedRequest = ClientRequest.from(request)
////                    .headers(headers -> {
//////                        headers.put("bg-field3", List.of("edited field 3"));
////                    })
////                    .build();
////            return next.exchange(modifiedRequest);
////        })
//                ;
//    }
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.baseUrl("http://localhost:8083").build();
    }

    /**
     * BUG in OtelPropagationConfiguration#BaggageTaggingConfiguration.
     * It cannot set tag fields into span from application properties.
     * Adding manually instead.
     * // TODO: need to check when migrate to Micrometer + SpringBoot 3.x
     * @return
     */
    @Bean
    BaggageTaggingSpanProcessor baggageTaggingSpanProcessor() {
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
        }
    }
}
