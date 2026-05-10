package com.oms.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.common.dto.ApiResponse;
import com.oms.common.dto.OrderDTO;
import com.oms.common.dto.OrderItemDTO;
import com.oms.common.enums.OrderStatus;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.OrderItemEvent;
import com.oms.order.dto.CreateOrderItemRequest;
import com.oms.order.dto.CreateOrderRequest;
import com.oms.order.dto.ValidateStockRequest;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderItem;
import com.oms.order.entity.User;
import com.oms.order.exception.InsufficientStockException;
import com.oms.order.exception.OrderNotFoundException;
import com.oms.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${services.inventory-url}")
    private String inventoryServiceUrl;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, User currentUser) {
        log.info("Creating order for user={}", currentUser.getUsername());

        // 1. Validate stock with inventory service
        validateStock(request.getItems());

        // 2. Fetch product details & calculate prices from inventory service
        List<OrderItem> orderItems = buildOrderItems(request.getItems());
        BigDecimal total = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Persist order as PENDING
        Order order = Order.builder()
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);
        Order saved = orderRepository.save(order);

        log.info("Order persisted with id={} totalAmount={}", saved.getId(), saved.getTotalAmount());

        // 4. Publish OrderCreatedEvent to Kafka
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(saved.getId())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .items(orderItems.stream().map(this::toItemEvent).collect(Collectors.toList()))
                .totalAmount(saved.getTotalAmount())
                .createdAt(saved.getCreatedAt())
                .build();

        eventPublisher.publishOrderCreated(event);

        // 5. Cache the new order
        OrderDTO dto = toDTO(saved);
        redisTemplate.opsForValue().set("order:" + saved.getId(), dto);

        return dto;
    }

    @Cacheable(value = "orders", key = "#orderId")
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(String orderId, User currentUser) {
        log.debug("Fetching order id={} (cache miss - hitting DB)", orderId);
        Order order = orderRepository.findByIdAndUserId(orderId, currentUser.getId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getMyOrders(User currentUser, int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(currentUser.getId(), status, pageable);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        }
        return orders.map(this::toDTO);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderDTO cancelOrder(String orderId, User currentUser) {
        Order order = orderRepository.findByIdAndUserId(orderId, currentUser.getId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot cancel an order that is already " + order.getStatus());
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order is already cancelled");
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        eventPublisher.publishOrderStatusChanged(orderId, currentUser.getId(), previous, OrderStatus.CANCELLED);
        log.info("Order cancelled: id={} by user={}", orderId, currentUser.getUsername());

        return toDTO(order);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        eventPublisher.publishOrderStatusChanged(orderId, order.getUserId(), previous, newStatus);
        log.info("Order status updated: id={} {} -> {}", orderId, previous, newStatus);
    }

    private void validateStock(List<CreateOrderItemRequest> items) {
        try {
            ValidateStockRequest stockRequest = ValidateStockRequest.builder()
                    .items(items.stream()
                            .map(i -> ValidateStockRequest.StockItem.builder()
                                    .productId(i.getProductId())
                                    .quantity(i.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            restTemplate.postForObject(
                    inventoryServiceUrl + "/api/v1/inventory/validate",
                    stockRequest,
                    ResponseEntity.class
            );
        } catch (HttpClientErrorException e) {
            log.error("Stock validation failed: {}", e.getResponseBodyAsString());
            throw new InsufficientStockException("Stock validation failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.warn("Inventory service unavailable, proceeding with order: {}", e.getMessage());
            // Continue order creation if inventory service is temporarily down (graceful degradation)
        }
    }

    private List<OrderItem> buildOrderItems(List<CreateOrderItemRequest> items) {
        return items.stream().map(req -> {
            // Fetch product details from inventory service
            BigDecimal price = fetchProductPrice(req.getProductId());
            String productName = fetchProductName(req.getProductId());
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(req.getQuantity()));

            return OrderItem.builder()
                    .productId(req.getProductId())
                    .productName(productName)
                    .quantity(req.getQuantity())
                    .price(price)
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal fetchProductPrice(String productId) {
        try {
            ResponseEntity<java.util.Map> response = restTemplate.getForEntity(
                    inventoryServiceUrl + "/api/v1/inventory/products/" + productId,
                    java.util.Map.class
            );
            if (response.getBody() != null && response.getBody().get("data") != null) {
                java.util.Map data = (java.util.Map) response.getBody().get("data");
                return new BigDecimal(data.get("price").toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch price for product={}, using default", productId);
        }
        return BigDecimal.valueOf(100.00); // fallback default price
    }

    private String fetchProductName(String productId) {
        try {
            ResponseEntity<java.util.Map> response = restTemplate.getForEntity(
                    inventoryServiceUrl + "/api/v1/inventory/products/" + productId,
                    java.util.Map.class
            );
            if (response.getBody() != null && response.getBody().get("data") != null) {
                java.util.Map data = (java.util.Map) response.getBody().get("data");
                return data.get("name").toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch name for product={}", productId);
        }
        return "Product " + productId;
    }

    private OrderItemEvent toItemEvent(OrderItem item) {
        return OrderItemEvent.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    public OrderDTO toDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .username(order.getUsername())
                .status(order.getStatus())
                .items(itemDTOs)
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
