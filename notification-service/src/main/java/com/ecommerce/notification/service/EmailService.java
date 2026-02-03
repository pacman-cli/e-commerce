package com.ecommerce.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class EmailService {

    public void sendWelcomeEmail(String email, String fullName) {
        log.info("Sending welcome email to: {} for user: {}", email, fullName);
    }

    public void sendOrderConfirmationEmail(String email, String orderId, BigDecimal totalAmount) {
        log.info("Sending order confirmation email to: {} for order: {} with total: ${}", 
                email, orderId, totalAmount);
    }

    public void sendPaymentReceiptEmail(String email, String orderId, BigDecimal amount, String transactionId) {
        log.info("Sending payment receipt email to: {} for order: {} - Amount: ${}, Transaction: {}", 
                email, orderId, amount, transactionId);
    }

    public void sendPaymentFailureAlert(String email, String orderId, BigDecimal amount) {
        log.warn("Sending payment failure alert to: {} for order: {} - Failed Amount: ${}", 
                email, orderId, amount);
    }
}
