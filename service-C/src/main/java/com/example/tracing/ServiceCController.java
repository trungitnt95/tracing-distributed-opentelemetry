package com.example.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/v2")
public class ServiceCController {

    private static final Logger log = LoggerFactory.getLogger(ServiceCController.class);

    @Autowired
    private Tracer tracer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClient webClient;


    @Autowired
    private JmsTemplate jmsTemplate;


    public void sendMessage(String queueName, final String message) {

        System.out.println("Gửi tin nhắn đến queue - " + queueName);
        jmsTemplate.convertAndSend(queueName, message);

    }

    @GetMapping("/ex0/users/{id}/profiles")
    public String test0(@PathVariable("id") String userId) {
        log.info("ex0 {}", userId);
        return "{id: " + userId +", profiles: ['p0','p1']}";
    }

    @GetMapping("/ex1/users/{id}/profiles")
    public String test1(@PathVariable("id") String userId) {
        log.info("ex1 {}", userId);
        return "{id: " + userId +", profiles: ['p1','p2']}";
    }

    @GetMapping("/ex2/users/{id}/profiles")
    public String test2(@PathVariable("id") String userId) {
        log.info("ex2 {}", userId);
        return "{id: " + userId +", profiles: ['p1','p2']}";
    }

    @GetMapping("/ex3/users/{id}/profiles")
    public Mono<String> test3(@PathVariable("id") String userId) {
        log.info("ex3 {}", userId);
        return Mono.just("{id: " + userId +", profiles: ['p1','p2']}");
    }

    @GetMapping("/ex4/users/{id}/profiles")
    public Mono<String> test4(@PathVariable("id") String userId) {
        log.info("ex4 {}", userId);
        return Mono.just("{id: " + userId +", profiles: ['p1','p2']}");
    }

    @GetMapping("/ex5/users/{id}/profiles")
    public Flux<String> test5(@PathVariable("id") String userId) {
        log.info("ex5 {}", userId);
        return Flux.just("{id: " + userId +", profiles: ['p1','p2']}","{id: " + userId +", profiles: ['p3','p4']}","{id: " + userId +", profiles: ['p5','p6']}")
                //.delayElements(Duration.ofSeconds(5))
                ;
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