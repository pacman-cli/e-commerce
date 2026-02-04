package com.ecommerce.payment.service;

import java.util.Optional;
import java.util.UUID;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.entity.PaymentStatus;

public interface PaymentService {
  PaymentResponse processPayment(ProcessPaymentRequest request);

  PaymentStatus simulatePaymentProcessing();

  Optional<PaymentResponse> getPaymentByOrderId(UUID orderId);
}
