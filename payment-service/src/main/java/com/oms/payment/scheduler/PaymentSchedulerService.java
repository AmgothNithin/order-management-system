package com.oms.payment.scheduler;

import com.oms.common.enums.PaymentStatus;
import com.oms.payment.entity.Payment;
import com.oms.payment.repository.PaymentRepository;
import com.oms.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSchedulerService {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * Retry failed payments up to 3 times.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000)
    public void retryFailedPayments() {
        List<Payment> failedPayments = paymentRepository
                .findByStatusAndRetryCountLessThan(PaymentStatus.FAILED, 3);

        if (failedPayments.isEmpty()) {
            log.debug("No failed payments to retry");
            return;
        }

        log.info("Retrying {} failed payments", failedPayments.size());
        failedPayments.forEach(payment -> {
            try {
                paymentService.retryPayment(payment);
            } catch (Exception e) {
                log.error("Error retrying paymentId={}: {}", payment.getId(), e.getMessage());
            }
        });
    }
}
