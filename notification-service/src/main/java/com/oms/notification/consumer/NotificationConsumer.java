package com.oms.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.OrderStatusChangedEvent;
import com.oms.common.events.PaymentProcessedEvent;
import com.oms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @KafkaListener(topics = "order.created", groupId = "notification-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Notification: order.created for orderId={}", record.key());
        try {
            OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
            notificationService.handleOrderCreated(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.created notification for orderId={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "payment.processed", groupId = "notification-group")
    public void handlePaymentProcessed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Notification: payment.processed for orderId={}", record.key());
        try {
            PaymentProcessedEvent event = objectMapper.readValue(record.value(), PaymentProcessedEvent.class);
            notificationService.handlePaymentProcessed(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment.processed notification for orderId={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "order.status.changed", groupId = "notification-group")
    public void handleOrderStatusChanged(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Notification: order.status.changed for orderId={}", record.key());
        try {
            OrderStatusChangedEvent event = objectMapper.readValue(record.value(), OrderStatusChangedEvent.class);
            notificationService.handleOrderStatusChanged(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.status.changed notification for orderId={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
