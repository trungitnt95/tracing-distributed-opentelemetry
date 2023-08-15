package com.example.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v2")
public class ServiceBController {

    private static final Logger log = LoggerFactory.getLogger(ServiceBController.class);

    @Autowired
    private Tracer tracer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClient webClient;


    @Autowired
    private JmsTemplate jmsTemplate;


    @PostMapping(path = "/ex0/testing")
    public Flux<String> ge1t(@RequestBody Flux<TestAA> req) {
        return req.flatMap(t -> Mono.just("BBB"));
    }

    @GetMapping("/ex0/users/{id}")
    public ResponseEntity<String> ex0(@PathVariable("id") String id) {
        log.info("ex0 {}", id);
        ResponseEntity<String> bToC =
                restTemplate.getForEntity("http://localhost:8083/v2/ex0/users/" + id + "/profiles", String.class);
        ResponseEntity<String> aToD =
                restTemplate.getForEntity("http://localhost:8084/v3/ex0/users/usid-001", String.class);
        return ResponseEntity.ok(bToC + ":" + aToD);
    }

    @GetMapping("/ex1/users/{id}")
    public ResponseEntity<String> ex1(@PathVariable("id") String id) {
        log.info("ex1 {}", id);
        return restTemplate.getForEntity("http://localhost:8083/v2/ex1/users/" + id + "/profiles", String.class);
    }

    @GetMapping("/ex3/users/{id}")
    public Mono<String> ex3(@PathVariable("id") String id) throws InterruptedException {
        log.info("ex3 {}", id);
        return webClient.get().uri("http://localhost:8083/v2/ex3/users/" + id + "/profiles")
                .retrieve().bodyToMono(String.class);
    }
    @GetMapping("/ex01/users/{id}")
    public Mono<String> ex01(@PathVariable("id") String id) throws InterruptedException {
        log.info("ex3 {}", id);
        Mono<String> bToC = webClient.get().uri("http://localhost:8083/v2/ex3/users/" + id + "/profiles")
                .retrieve().bodyToMono(String.class);

        Mono<String> bToD = webClient.get().uri("http://localhost:8084/v3/ex01/users/" + id + "/profiles")
                .retrieve().bodyToMono(String.class);
//        return bToC;
        return Mono.zip(bToC, bToD).map(tuple -> {
            String resultFromA = tuple.getT1();
            String resultFromB = tuple.getT2();

            // Process the results from both Monos and return the merged result
            String mergedResult = resultFromA + resultFromB;
            return mergedResult;
        });
    }

    @GetMapping("/ex5/users/{id}")
    public Flux<String> ex5(@PathVariable("id") String id) {
        log.info("ex5 {}", id);
        return webClient.get().uri("http://localhost:8083/v2/ex5/users/" + id + "/profiles")
                .retrieve().bodyToFlux(String.class);
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
        ResponseEntity<String> first = restTemplate.getForEntity("http://localhost:8083/test1", String.class);

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


//    @JmsListener(destination = "inbound.queue")
//    public void receiveMessage(Message jsonMessage) throws JMSException {
//
//
//
//        String messageData = null;
//        if(jsonMessage instanceof TextMessage) {
//            TextMessage textMessage = (TextMessage)jsonMessage;
//            messageData = textMessage.getText();
//        }
//
////        producer.sendMessage("outbound.queue", messageData);
//    }
}