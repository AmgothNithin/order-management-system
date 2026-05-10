package com.oms.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oms.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private String eventId;
    private String orderId;
    private String userId;
    private String paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String failureReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;
}
