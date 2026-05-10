package com.oms.notification.service;

import com.oms.common.enums.NotificationType;
import com.oms.common.enums.OrderStatus;
import com.oms.common.enums.PaymentStatus;
import com.oms.common.events.OrderCreatedEvent;
import com.oms.common.events.OrderStatusChangedEvent;
import com.oms.common.events.PaymentProcessedEvent;
import com.oms.notification.entity.Notification;
import com.oms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String message = String.format(
                "Hello %s! Your order #%s has been placed successfully. Total: ₹%.2f. We are processing your payment.",
                event.getUsername(), event.getOrderId(), event.getTotalAmount());

        saveAndSend(event.getOrderId(), event.getUserId(), event.getUsername(),
                NotificationType.ORDER_CREATED, message);
    }

    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        NotificationType type;
        String message;

        if (event.getStatus() == PaymentStatus.SUCCESS) {
            type = NotificationType.PAYMENT_SUCCESS;
            message = String.format(
                    "Payment of ₹%.2f confirmed for order #%s. Your order is now being prepared.",
                    event.getAmount(), event.getOrderId());
        } else {
            type = NotificationType.PAYMENT_FAILED;
            message = String.format(
                    "Payment failed for order #%s. Reason: %s. Please retry or use a different payment method.",
                    event.getOrderId(), event.getFailureReason() != null ? event.getFailureReason() : "Unknown");
        }

        saveAndSend(event.getOrderId(), event.getUserId(), null, type, message);
    }

    @Transactional
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        NotificationType type = mapStatusToNotificationType(event.getNewStatus());
        if (type == null) return;

        String message = buildStatusMessage(event);
        saveAndSend(event.getOrderId(), event.getUserId(), null, type, message);
    }

    public List<Notification> getNotificationsByUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Page<Notification> getNotificationsByUserPaged(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<Notification> getNotificationsByOrder(String orderId) {
        return notificationRepository.findByOrderId(orderId);
    }

    private void saveAndSend(String orderId, String userId, String username,
                              NotificationType type, String message) {
        Notification notification = Notification.builder()
                .orderId(orderId)
                .userId(userId)
                .username(username)
                .notificationType(type)
                .message(message)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        // In production: integrate with email/SMS/push provider here
        log.info("[NOTIFICATION SENT] type={} userId={} orderId={} | {}", type, userId, orderId, message);
    }

    private NotificationType mapStatusToNotificationType(OrderStatus status) {
        return switch (status) {
            case SHIPPED -> NotificationType.ORDER_SHIPPED;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            default -> null;
        };
    }

    private String buildStatusMessage(OrderStatusChangedEvent event) {
        return switch (event.getNewStatus()) {
            case SHIPPED -> String.format("Great news! Your order #%s has been shipped and is on its way.", event.getOrderId());
            case DELIVERED -> String.format("Your order #%s has been delivered. Enjoy!", event.getOrderId());
            case CANCELLED -> String.format("Your order #%s has been cancelled. Refund will be processed within 5-7 business days.", event.getOrderId());
            default -> String.format("Your order #%s status has been updated to %s.", event.getOrderId(), event.getNewStatus());
        };
    }
}
