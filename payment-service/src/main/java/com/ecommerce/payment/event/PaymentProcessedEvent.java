package com.ecommerce.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.ecommerce.payment.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
  private UUID orderId;
  private UUID paymentId;
  private PaymentStatus status;
  private String transactionId;
  private BigDecimal amount;
  private String userEmail;
}
