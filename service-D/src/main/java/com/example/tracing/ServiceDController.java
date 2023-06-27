package com.example.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v3")
public class ServiceDController {

    @Autowired
    private Tracer tracer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(ServiceDController.class);

    @Autowired
    private JmsTemplate jmsTemplate;

//    @Autowired
//    private ObservationRegistry observationRegistry;


    @GetMapping("/ex0/users/{id}")
    public ResponseEntity<String> ex0(@PathVariable("id") String id) {
        log.info("ex0 {}", id);
        return ResponseEntity.ok("D service result");
    }

    @GetMapping("/ex2/users/{id}")
    public ResponseEntity<String> ex1(@PathVariable("id") String id) {
        log.info("ex1 {}", id);
        return restTemplate.getForEntity("http://localhost:8083/v2/ex1/users/" + id + "/profiles", String.class);
    }

    @GetMapping("/ex01/users/{id}/profiles")
    public Mono<String> test01(@PathVariable("id") String userId) {
        log.info("ex01 {}", userId);
        return Mono.just("{id: " + userId +", profiles: ['p1','p2']}");
    }

    @GetMapping("/ex4/users/{id}")
    public Mono<String> ex3(@PathVariable("id") String id) {
        log.info("ex4 {}", id);
        return webClient.get().uri("http://localhost:8083/v2/ex4/users/" + id + "/profiles")
                .retrieve().bodyToMono(String.class);
    }

    @GetMapping("/ex11/get-numbers")
    @CrossOrigin(origins = "http://localhost:4200")
    public Flux<Integer> ex11() {
        return webClient.get().uri("http://localhost:8085/v3/ex11/get-numbers")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve().bodyToFlux(Integer.class);
    }

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

        Mono<String> stringMono = webClient
                .get()
                .uri("http://localhost:8083/test2")
                .header("bg-field2", "")
                .retrieve().bodyToMono(String.class);
        stringMono.subscribe();

        Span span = tracer.currentSpan();
        this.sendMessage("inbound.queue", "I am a HACKER!!!!");
        return "Hello World!";
    }


}