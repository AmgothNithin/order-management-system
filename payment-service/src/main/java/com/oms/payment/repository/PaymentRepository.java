package com.oms.payment.repository;

import com.oms.common.enums.PaymentStatus;
import com.oms.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByStatusAndRetryCountLessThan(PaymentStatus status, int maxRetryCount);
    boolean existsByOrderId(String orderId);
}
