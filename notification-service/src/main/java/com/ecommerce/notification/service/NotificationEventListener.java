package com.ecommerce.notification.service;

import com.ecommerce.shared.events.OrderCreatedEvent;
import com.ecommerce.shared.events.PaymentProcessedEvent;
import com.ecommerce.shared.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for user: {} ({})", event.getEmail(), event.getUserId());
        emailService.sendWelcomeEmail(event.getEmail(), event.getFullName());
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {} from user: {}", 
                event.getOrderId(), event.getUserEmail());
        emailService.sendOrderConfirmationEmail(
                event.getUserEmail(), 
                event.getOrderId().toString(), 
                event.getTotalAmount()
        );
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for order: {} - Status: {}", 
                event.getOrderId(), event.getStatus());
        
        if ("COMPLETED".equalsIgnoreCase(event.getStatus())) {
            emailService.sendPaymentReceiptEmail(
                    event.getUserEmail(),
                    event.getOrderId().toString(),
                    event.getAmount(),
                    event.getTransactionId()
            );
        } else {
            emailService.sendPaymentFailureAlert(
                    event.getUserEmail(),
                    event.getOrderId().toString(),
                    event.getAmount()
            );
        }
    }
}
