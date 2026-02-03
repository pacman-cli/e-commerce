package com.ecommerce.order.service;

import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvent(PaymentCompletedEvent event) {
        log.info("Received payment event for order: {}, success: {}", 
                event.getOrderId(), event.isSuccess());

        OrderStatus newStatus = event.isSuccess() ? OrderStatus.PAID : OrderStatus.CANCELLED;
        orderService.updateOrderStatus(event.getOrderId(), newStatus);
        
        log.info("Order {} status updated to {}", event.getOrderId(), newStatus);
    }
}
