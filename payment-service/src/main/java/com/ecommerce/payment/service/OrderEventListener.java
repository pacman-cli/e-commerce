package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.shared.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-events", groupId = "payment-service-group")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {} with amount: {}", 
                   event.getOrderId(), event.getTotalAmount());

        ProcessPaymentRequest request = new ProcessPaymentRequest(
            event.getOrderId(),
            event.getTotalAmount(),
            event.getUserEmail()
        );

        paymentService.processPayment(request);
    }
}
