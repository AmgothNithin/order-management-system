package com.oms.notification.repository;

import com.oms.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserId(String userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<Notification> findByOrderId(String orderId);
    long countByUserIdAndIsSent(String userId, boolean isSent);
}
