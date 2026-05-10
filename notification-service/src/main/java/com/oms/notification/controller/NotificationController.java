package com.oms.notification.controller;

import com.oms.common.dto.ApiResponse;
import com.oms.notification.entity.Notification;
import com.oms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<Notification>>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotificationsByUserPaged(userId, page, size)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getOrderNotifications(
            @PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotificationsByOrder(orderId)));
    }
}
