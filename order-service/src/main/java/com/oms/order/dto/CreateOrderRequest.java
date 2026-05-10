package com.oms.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<CreateOrderItemRequest> items;

    private String shippingAddress;
    private String notes;
}
