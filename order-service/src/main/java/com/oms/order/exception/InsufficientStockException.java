package com.oms.order.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productId) {
        super("Insufficient stock for product: " + productId);
    }
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
