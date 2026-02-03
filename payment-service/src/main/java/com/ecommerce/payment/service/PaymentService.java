package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment for order: {}, amount: {}", request.getOrderId(), request.getAmount());

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setUserEmail(request.getUserEmail());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {} and PENDING status", savedPayment.getId());

        PaymentStatus paymentStatus = simulatePaymentProcessing();
        
        savedPayment.setStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.SUCCESS) {
            savedPayment.setTransactionId(UUID.randomUUID().toString());
        }
        
        Payment updatedPayment = paymentRepository.save(savedPayment);
        log.info("Payment {} processed with status: {}", updatedPayment.getId(), paymentStatus);

        PaymentProcessedEvent event = new PaymentProcessedEvent(
            updatedPayment.getOrderId(),
            updatedPayment.getId(),
            paymentStatus,
            updatedPayment.getTransactionId(),
            updatedPayment.getAmount(),
            updatedPayment.getUserEmail()
        );
        
        kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event);
        log.info("PaymentProcessedEvent published to Kafka for order: {}", request.getOrderId());

        return mapToResponse(updatedPayment);
    }

    public PaymentStatus simulatePaymentProcessing() {
        int randomValue = random.nextInt(100);
        return randomValue < 90 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::mapToResponse);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getTransactionId(),
            payment.getUserEmail(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }

    public static class PaymentProcessedEvent {
        private UUID orderId;
        private UUID paymentId;
        private PaymentStatus status;
        private String transactionId;
        private BigDecimal amount;
        private String userEmail;

        public PaymentProcessedEvent() {
        }

        public PaymentProcessedEvent(UUID orderId, UUID paymentId, PaymentStatus status, 
                                     String transactionId, BigDecimal amount, String userEmail) {
            this.orderId = orderId;
            this.paymentId = paymentId;
            this.status = status;
            this.transactionId = transactionId;
            this.amount = amount;
            this.userEmail = userEmail;
        }

        public UUID getOrderId() {
            return orderId;
        }

        public void setOrderId(UUID orderId) {
            this.orderId = orderId;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(UUID paymentId) {
            this.paymentId = paymentId;
        }

        public PaymentStatus getStatus() {
            return status;
        }

        public void setStatus(PaymentStatus status) {
            this.status = status;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }
    }
}
