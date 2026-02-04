package com.ecommerce.payment.service.impl;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.event.PaymentProcessedEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

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
                updatedPayment.getUserEmail());

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
                payment.getUpdatedAt());
    }
}
