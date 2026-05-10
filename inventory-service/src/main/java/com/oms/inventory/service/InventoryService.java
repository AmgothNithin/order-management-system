package com.oms.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.common.events.InventoryUpdatedEvent;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.OrderItemEvent;
import com.oms.inventory.entity.InventoryTransaction;
import com.oms.inventory.entity.Product;
import com.oms.inventory.repository.InventoryTransactionRepository;
import com.oms.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    public static final String TOPIC_INVENTORY_UPDATED = "inventory.updated";

    @Transactional
    public void decrementStockForOrder(OrderCreatedEvent event) {
        List<OrderItemEvent> items = event.getItems();
        for (OrderItemEvent item : items) {
            // Idempotency check
            if (transactionRepository.existsByOrderIdAndProductId(event.getOrderId(), item.getProductId())) {
                log.warn("Stock already decremented for orderId={} productId={}", event.getOrderId(), item.getProductId());
                continue;
            }

            int updated = productRepository.decrementStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                log.error("Insufficient stock for productId={} qty={} — reverting order", item.getProductId(), item.getQuantity());
                revertPreviousDeductions(event.getOrderId(), items, item.getProductId());
                publishOrderCancelled(event.getOrderId());
                return;
            }

            transactionRepository.save(InventoryTransaction.builder()
                    .orderId(event.getOrderId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .transactionType("DEDUCT")
                    .build());

            log.info("Stock decremented: productId={} qty={} orderId={}", item.getProductId(), item.getQuantity(), event.getOrderId());
        }

        publishInventoryUpdated(event);
    }

    @Transactional
    public void revertStockForOrder(String orderId) {
        List<InventoryTransaction> transactions = transactionRepository.findByOrderId(orderId);
        transactions.stream()
                .filter(t -> "DEDUCT".equals(t.getTransactionType()))
                .forEach(t -> {
                    productRepository.incrementStock(t.getProductId(), t.getQuantity());
                    transactionRepository.save(InventoryTransaction.builder()
                            .orderId(orderId)
                            .productId(t.getProductId())
                            .quantity(t.getQuantity())
                            .transactionType("REVERT")
                            .build());
                    log.info("Stock reverted: productId={} qty={} orderId={}", t.getProductId(), t.getQuantity(), orderId);
                });
    }

    public void validateStock(Map<String, Integer> productQuantities) {
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + entry.getKey()));
            if (product.getAvailableQuantity() < entry.getValue()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                        " (available: " + product.getAvailableQuantity() + ", requested: " + entry.getValue() + ")");
            }
        }
    }

    public Product getProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    private void revertPreviousDeductions(String orderId, List<OrderItemEvent> items, String failedProductId) {
        items.stream()
                .filter(i -> !i.getProductId().equals(failedProductId))
                .filter(i -> transactionRepository.existsByOrderIdAndProductId(orderId, i.getProductId()))
                .forEach(i -> {
                    productRepository.incrementStock(i.getProductId(), i.getQuantity());
                    log.info("Reverted stock for productId={} qty={}", i.getProductId(), i.getQuantity());
                });
    }

    private void publishOrderCancelled(String orderId) {
        try {
            kafkaTemplate.send(TOPIC_ORDER_CANCELLED, orderId, orderId);
            log.info("Published order.cancelled for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish order.cancelled for orderId={}", orderId, e);
        }
    }

    private void publishInventoryUpdated(OrderCreatedEvent event) {
        try {
            InventoryUpdatedEvent updatedEvent = InventoryUpdatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .items(event.getItems())
                    .updatedAt(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(TOPIC_INVENTORY_UPDATED, event.getOrderId(), objectMapper.writeValueAsString(updatedEvent));
            log.info("Published inventory.updated for orderId={}", event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish inventory.updated for orderId={}", event.getOrderId(), e);
        }
    }
}
