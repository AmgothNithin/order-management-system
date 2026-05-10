package com.oms.notification.entity;

import com.oms.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_order_id", columnList = "order_id"),
    @Index(name = "idx_notif_user_id", columnList = "user_id"),
    @Index(name = "idx_notif_type", columnList = "notification_type")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "username")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "is_sent")
    @Builder.Default
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
