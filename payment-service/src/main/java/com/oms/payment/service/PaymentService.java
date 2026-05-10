package com.oms.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.common.enums.PaymentStatus;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.PaymentProcessedEvent;
import com.oms.payment.entity.Payment;
import com.oms.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${payment.success-rate:0.9}")
    private double successRate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static final String TOPIC_PAYMENT_PROCESSED = "payment.processed";

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        // Idempotency check
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Payment already processed for orderId={}", event.getOrderId());
            return;
        }

        log.info("Processing payment for orderId={} amount={}", event.getOrderId(), event.getTotalAmount());

        // Simulate payment processing (90% success rate)
        boolean isSuccess = Math.random() < successRate;

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .status(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .transactionReference(UUID.randomUUID().toString())
                .failureReason(isSuccess ? null : "Simulated payment gateway decline")
                .processedAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment {} for orderId={}", payment.getStatus(), event.getOrderId());

        publishPaymentResult(payment, event.getUserId());
    }

    @Transactional
    public void retryPayment(Payment payment) {
        if (payment.getRetryCount() >= 3) {
            log.warn("Max retries reached for paymentId={}", payment.getId());
            return;
        }

        log.info("Retrying payment id={} orderId={} attempt={}", 
                  payment.getId(), payment.getOrderId(), payment.getRetryCount() + 1);

        boolean isSuccess = Math.random() < successRate;
        payment.incrementRetryCount();
        payment.setStatus(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setProcessedAt(LocalDateTime.now());

        if (isSuccess) {
            payment.setTransactionReference(UUID.randomUUID().toString());
            payment.setFailureReason(null);
        }

        paymentRepository.save(payment);
        publishPaymentResult(payment, payment.getUserId());
        log.info("Payment retry result: {} for orderId={}", payment.getStatus(), payment.getOrderId());
    }

    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private void publishPaymentResult(Payment payment, String userId) {
        try {
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(payment.getOrderId())
                    .userId(userId)
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .amount(payment.getAmount())
                    .failureReason(payment.getFailureReason())
                    .processedAt(payment.getProcessedAt())
                    .build();

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_PAYMENT_PROCESSED, payment.getOrderId(), json);
            log.info("Published payment.processed event for orderId={} status={}", 
                      payment.getOrderId(), payment.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish payment result for orderId={}", payment.getOrderId(), e);
        }
    }
}
