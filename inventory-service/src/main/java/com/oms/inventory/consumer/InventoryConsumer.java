package com.oms.inventory.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.common.enums.PaymentStatus;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.PaymentProcessedEvent;
import com.oms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {

    private final InventoryService inventoryService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @KafkaListener(topics = "payment.processed", groupId = "inventory-group")
    public void handlePaymentProcessed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received payment.processed for orderId={}", record.key());
        try {
            PaymentProcessedEvent event = objectMapper.readValue(record.value(), PaymentProcessedEvent.class);
            if (event.getStatus() == PaymentStatus.SUCCESS) {
                // We need original order items — listen to order.created as well and cache, or re-fetch
                // For simplicity, inventory was already validated on order creation; deduction happens here
                log.info("Payment SUCCESS for orderId={} — stock already reserved on order.created", event.getOrderId());
            } else {
                log.info("Payment FAILED for orderId={} — reverting stock", event.getOrderId());
                inventoryService.revertStockForOrder(event.getOrderId());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling payment.processed for orderId={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "order.created", groupId = "inventory-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received order.created for orderId={}", record.key());
        try {
            OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
            inventoryService.decrementStockForOrder(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling order.created for orderId={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
