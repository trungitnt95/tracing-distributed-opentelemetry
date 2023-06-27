package com.example.server2;

import io.micrometer.tracing.Tracer;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class Example {

    @Autowired
    Tracer tracer;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(Example.class);
    @GetMapping("/test1")
    String home() {
        log.info("Hello world!");
//        ResponseEntity<String> first = restTemplate.getForEntity("http://localhost:8084/test1", String.class);

        return "Hello World!1";
    }

    @GetMapping("/test2")
    Mono<String> home1() {
        log.info("Hello world!");

//        Mono<String> stringMono = webClient.get().uri("http://localhost:8084/test2").retrieve().bodyToMono(String.class);
//        stringMono.subscribe();
        return Mono.just("Hello World!2");
    }

    @JmsListener(destination = "inbound.queue")
    public void receiveMessage(Message jsonMessage) throws JMSException {



        String messageData = null;
        if(jsonMessage instanceof TextMessage) {
            TextMessage textMessage = (TextMessage)jsonMessage;
            messageData = textMessage.getText();
        }

//        producer.sendMessage("outbound.queue", messageData);
    }
}
