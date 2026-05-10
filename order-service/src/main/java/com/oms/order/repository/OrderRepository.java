package com.oms.order.repository;

import com.oms.common.enums.OrderStatus;
import com.oms.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoff")
    List<Order> findStaleOrders(@Param("status") OrderStatus status,
                                @Param("cutoff") LocalDateTime cutoff);

    Optional<Order> findByIdAndUserId(String id, String userId);

    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus, o.updatedAt = :now WHERE o.id = :orderId")
    void updateStatus(@Param("orderId") String orderId,
                      @Param("newStatus") OrderStatus newStatus,
                      @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") OrderStatus status);
}
