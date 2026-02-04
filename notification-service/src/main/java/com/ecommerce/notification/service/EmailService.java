package com.ecommerce.notification.service;

import java.math.BigDecimal;

public interface EmailService {
  void sendWelcomeEmail(String email, String fullName);

  void sendOrderConfirmationEmail(String email, String orderId, BigDecimal totalAmount);

  void sendPaymentReceiptEmail(String email, String orderId, BigDecimal amount, String transactionId);

  void sendPaymentFailureAlert(String email, String orderId, BigDecimal amount);
}
