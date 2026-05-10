package com.oms.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oms.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {
    private String eventId;
    private String orderId;
    private String userId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
}
