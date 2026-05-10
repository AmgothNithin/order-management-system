package com.oms.order.controller;

import com.oms.common.dto.ApiResponse;
import com.oms.common.dto.OrderDTO;
import com.oms.common.enums.OrderStatus;
import com.oms.order.dto.CreateOrderRequest;
import com.oms.order.entity.User;
import com.oms.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User currentUser) {
        OrderDTO order = orderService.createOrder(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal User currentUser) {
        OrderDTO order = orderService.getOrderById(orderId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getMyOrders(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        Page<OrderDTO> orders = orderService.getMyOrders(currentUser, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal User currentUser) {
        OrderDTO order = orderService.cancelOrder(orderId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }
}
