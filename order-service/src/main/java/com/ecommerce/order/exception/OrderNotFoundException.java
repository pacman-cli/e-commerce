package com.ecommerce.order.exception;

/**
 * Exception thrown when an order cannot be found.
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String message) {
        super(message);
    }
    
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static OrderNotFoundException withId(String orderId) {
        return new OrderNotFoundException("Order not found with ID: " + orderId);
    }
}
