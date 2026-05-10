package com.oms.payment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @KafkaListener(topics = "order.created", groupId = "payment-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received order.created event for orderId={}", record.key());
        try {
            OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
            paymentService.processPayment(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.created for orderId={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge(); // Acknowledge to prevent poison pill; use DLQ in production
        }
    }
}
