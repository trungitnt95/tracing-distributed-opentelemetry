package com.example.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class ServiceCController {

    private static final Logger log = LoggerFactory.getLogger(ServiceCController.class);

    @Autowired
    Tracer tracer;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    WebClient.Builder webClient;


    @Autowired
    private JmsTemplate jmsTemplate;


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