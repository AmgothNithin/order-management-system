package com.oms.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.common.enums.OrderStatus;
import com.oms.common.enums.PaymentStatus;
import com.oms.common.events.PaymentProcessedEvent;
import com.oms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.processed", groupId = "order-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentProcessed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received payment.processed event for orderId={}", record.key());
        try {
            PaymentProcessedEvent event = objectMapper.readValue(record.value(), PaymentProcessedEvent.class);

            OrderStatus newStatus = event.getStatus() == PaymentStatus.SUCCESS
                    ? OrderStatus.CONFIRMED
                    : OrderStatus.PAYMENT_FAILED;

            orderService.updateOrderStatus(event.getOrderId(), newStatus);
            log.info("Order {} updated to status={}", event.getOrderId(), newStatus);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment.processed event for orderId={}: {}",
                    record.key(), e.getMessage(), e);
            // Acknowledge to prevent infinite retry loop; implement DLQ in production
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "order-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received order.cancelled event for orderId={}", record.key());
        try {
            orderService.updateOrderStatus(record.key(), OrderStatus.CANCELLED);
            log.info("Order {} marked as CANCELLED due to inventory failure", record.key());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to handle order.cancelled for orderId={}: {}", record.key(), e.getMessage());
            ack.acknowledge();
        }
    }
}
