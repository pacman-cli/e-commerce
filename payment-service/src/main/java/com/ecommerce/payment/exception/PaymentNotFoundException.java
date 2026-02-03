package com.ecommerce.payment.exception;

/**
 * Exception thrown when a payment cannot be found.
 */
public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
    
    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PaymentNotFoundException withOrderId(String orderId) {
        return new PaymentNotFoundException("Payment not found for order: " + orderId);
    }
}
