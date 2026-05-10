package com.oms.payment.controller;

import com.oms.common.dto.ApiResponse;
import com.oms.payment.entity.Payment;
import com.oms.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(payment -> ResponseEntity.ok(ApiResponse.success(payment)))
                .orElse(ResponseEntity.notFound().build());
    }
}
