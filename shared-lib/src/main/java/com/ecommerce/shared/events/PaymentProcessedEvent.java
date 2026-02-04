package com.ecommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private UUID orderId;
    private UUID paymentId;
    private String status;
    private String userEmail;
    private BigDecimal amount;
    private String transactionId;
    private long timestamp;
}
