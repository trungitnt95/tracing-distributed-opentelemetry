package com.example.server1;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@EnableAutoConfiguration
public class Example {

    @Autowired
    Tracer tracer;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    WebClient.Builder webClient;

    private static final Logger log = LoggerFactory.getLogger(Example.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObservationRegistry observationRegistry;


    public void sendMessage(String queueName, final String message) {

        System.out.println("Gửi tin nhắn đến queue - " + queueName);
//        jmsTemplate.send(queueName, new MessageCreator() {
//            public Message createMessage(Session session) throws JMSException {
//                TextMessage textMessage = session.createTextMessage(message);
////                textMessage.setStringProperty("x-span-id", jmsSpan.context().spanId());
////                textMessage.setStringProperty("x-trace-id", jmsSpan.context().traceId());
////                textMessage.setStringProperty("x-parent-span-id", currentContext != null ? currentContext.spanId() : null);
////                textMessage.setBooleanProperty("x-tracing-sampled", jmsSpan.context().sampled());
//                return textMessage;
//            }
//        });
        jmsTemplate.convertAndSend(queueName, message);

    }

    @GetMapping("/test")
    String home() {
        log.info("Hello world!");
//        ResponseEntity<String> first = Observation.start("rest-template-sample", observationRegistry).observe(() -> {
//            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
//            return this.restTemplate.getForObject(url, String.class);
//            return
                    restTemplate.getForEntity("http://localhost:8083/test1", String.class);
//        });

        Mono<String> stringMono = webClient.build()
                .get()
                .uri("http://localhost:8083/test2")
                .header("bg-field2", "")
                .retrieve().bodyToMono(String.class);
        stringMono.subscribe();

        Span span = tracer.currentSpan();
        this.sendMessage("inbound.queue", "I am a HACKER!!!!");
        return "Hello World!";
    }

    public static void main(String[] args) {
        SpringApplication.run(Example.class, args);
    }

}