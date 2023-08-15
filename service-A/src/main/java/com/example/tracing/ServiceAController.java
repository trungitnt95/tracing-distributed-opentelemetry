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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v2")
public class ServiceAController {

    private static final Logger log = LoggerFactory.getLogger(ServiceAController.class);

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

    @GetMapping("/ex0/http-rest-template")
    public ResponseEntity<String> ex0() {
        log.info("===== Exercise 0 =====");

        webClient.post().uri("http://localhost:8082/v2/ex0/testing")
                .body(Flux.just(new TestAA()), TestAA.class)

                .retrieve().bodyToFlux(String.class)
                .subscribe((str) -> {
                    System.out.println("OK- " + str);
                });

//        ResponseEntity<String> aToBToC = restTemplate.getForEntity("http://localhost:8082/v2/ex0/users/usid-001", String.class);
//        ResponseEntity<String> aToD = restTemplate.getForEntity("http://localhost:8085/v3/ex0/users/usid-001", String.class);


        return ResponseEntity.ok("hello world");
    }

    @GetMapping("/ex01/http-web-client")
    public ResponseEntity<String> test3() {
        log.info("===== Exercise 3 =====");
        Mono<String> resBody = webClient.get().uri("http://localhost:8082/v2/ex01/users/usid-001")
                .retrieve().bodyToMono(String.class);
        resBody.subscribe((next) -> {
            log.info("ex3 received {}", next);
        });
        webClient.get().uri("http://localhost:8085/v3/ex01/users/usid-001")
                .retrieve().bodyToMono(String.class)
                .subscribe((res) -> log.info("ex3 received {}", res));

        return ResponseEntity.ok("finished ex3");
    }

    @GetMapping("/ex1/http-rest-template")
    public ResponseEntity<String> ex1() {
        log.info("===== Exercise 1 =====");
        return restTemplate.getForEntity("http://localhost:8082/v2/ex1/users/usid-001", String.class);
    }

    @GetMapping("/ex2/http-rest-template")
    public ResponseEntity<String> ex2() {
        log.info("===== Exercise 2 =====");
        return restTemplate.getForEntity("http://localhost:8084/v3/ex2/users/usid-001", String.class);
    }

    @GetMapping("/ex3/http-web-client")
    public ResponseEntity<String> ex3() {
        log.info("===== Exercise 3 =====");
        Mono<String> resBody = webClient.get().uri("http://localhost:8082/v2/ex3/users/usid-001")
                .retrieve().bodyToMono(String.class);
        resBody.subscribe((next) -> {
            log.info("ex3 received {}", next);
        });
        return ResponseEntity.ok("finished ex3");
    }

    @GetMapping("/ex4/http-web-client")
    public ResponseEntity<String> ex4() {
        log.info("===== Exercise 4 =====");
        String result = webClient.get().uri("http://localhost:8082/v2/ex3/users/usid-001")
                .retrieve().bodyToMono(String.class)
                .block();
        log.info("result received {}", result);
        return ResponseEntity.ok("finished ex4" + result);
    }

    @GetMapping("/ex5/http-web-client")
    public ResponseEntity<String> ex5() {
        log.info("===== Exercise 5 =====");
        Mono<String> resBody = webClient.get().uri("http://localhost:8082/v2/ex5/users/usid-001")
                .retrieve().bodyToMono(String.class);
        resBody.subscribe((next) -> {
            log.info("ex5 received {}", next);
        });
        return ResponseEntity.ok("finished ex5");
    }

    @GetMapping("/ex6/http-rest-template")
    public ResponseEntity<String> ex6() {
        log.info("===== Exercise 6 =====");

        new Thread(() -> {
            ResponseEntity<String> forEntity = restTemplate.getForEntity("http://localhost:8082/v2/ex1/users/usid-001", String.class);
            log.info("response ex6 {}", forEntity.getBody());
        }).start();

        return ResponseEntity.ok("finished ex6");
    }

    @GetMapping("/test")
    public String home() {
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