package com.oms.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.common.enums.OrderStatus;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public static final String TOPIC_ORDER_CREATED = "order.created";
    public static final String TOPIC_ORDER_STATUS_CHANGED = "order.status.changed";

    public void publishOrderCreated(OrderCreatedEvent event) {
        sendEvent(TOPIC_ORDER_CREATED, event.getOrderId(), event);
        log.info("Published order.created event for orderId={}", event.getOrderId());
    }

    public void publishOrderStatusChanged(String orderId, String userId,
                                           OrderStatus previousStatus, OrderStatus newStatus) {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .userId(userId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .build();
        sendEvent(TOPIC_ORDER_STATUS_CHANGED, orderId, event);
        log.info("Published order.status.changed event: orderId={} {} -> {}", orderId, previousStatus, newStatus);
    }

    private void sendEvent(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, json);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send event to topic={} key={}: {}", topic, key, ex.getMessage());
                } else {
                    log.debug("Event sent to topic={} partition={} offset={}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic={}: {}", topic, e.getMessage());
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
