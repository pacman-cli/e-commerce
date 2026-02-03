package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.exception.InvalidOrderException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.metrics.OrderMetrics;
import com.ecommerce.order.outbox.OutboxEventPublisher;
import com.ecommerce.order.repository.OrderRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventPublisher outboxPublisher;
    private final OrderMetrics orderMetrics;

    @Transactional
    @Caching(
        evict = @CacheEvict(value = "orderList", key = "#userId + '_0'"),
        put = @CachePut(value = "orders", key = "#result.id()")
    )
    @CircuitBreaker(name = "orderCreationCircuitBreaker", fallbackMethod = "createOrderFallback")
    @Retry(name = "databaseRetry")
    @Bulkhead(name = "orderCreationBulkhead")
    @TimeLimiter(name = "orderCreationTimeout")
    public CompletableFuture<OrderResponse> createOrder(UUID userId, String userEmail, CreateOrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            orderMetrics.incrementActiveOrders();
            
            // Creates order; publishes event; records metrics; handles exceptions
            try {
                log.info("Creating order for user: {}", userId);

                Order order = Order.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .status(OrderStatus.CREATED)
                        .build();

                List<OrderItem> items = request.getItems().stream()
                        // Maps item requests to order item details
                        .map(itemRequest -> OrderItem.builder()
                                .productId(itemRequest.getProductId())
                                .quantity(itemRequest.getQuantity())
                                .price(itemRequest.getPrice())
                                .order(order)
                                .build())
                        .collect(Collectors.toList());

                items.forEach(order::addItem);
                order.calculateTotal();

                Order savedOrder = orderRepository.save(order);
                log.info("Order saved with id: {}", savedOrder.getId());

                // Record business metrics
                orderMetrics.recordOrderCreated();
                orderMetrics.recordHighValueOrder(savedOrder.getTotalAmount());
                orderMetrics.incrementCreatedOrders();
                orderMetrics.addToTotalValue(savedOrder.getTotalAmount());

                // Create an event payload
                OrderCreatedEvent event = OrderCreatedEvent.builder()
                        .orderId(savedOrder.getId())
                        .userId(savedOrder.getUserId())
                        .userEmail(savedOrder.getUserEmail())
                        .totalAmount(savedOrder.getTotalAmount())
                        .items(mapItemsToEvent(savedOrder))
                        .build();

                // Save event to outbox (same transaction as order save)
                // The outbox poller will then publish to Kafka asynchronously
                outboxPublisher.publish(
                    "Order",
                    savedOrder.getId().toString(),
                    "OrderCreated",
                    event
                );
                
                log.info("OrderCreatedEvent saved to outbox for order: {}", savedOrder.getId());

                return mapToOrderResponse(savedOrder);
            } catch (Exception e) {
                orderMetrics.recordOrderCreationFailure();
                throw e;
            } finally {
                Duration duration = Duration.between(start, Instant.now());
                orderMetrics.recordOrderCreation(duration);
                orderMetrics.decrementActiveOrders();
            }
        });
    }

    /**
     * Fallback method for createOrder when circuit breaker is open
     * Returns a failed CompletableFuture with user-friendly error
     */
    private CompletableFuture<OrderResponse> createOrderFallback(
            UUID userId, String userEmail, CreateOrderRequest request, Exception exception) {
        log.error("Order creation failed for user {} - circuit breaker open or retry exhausted", userId, exception);
        orderMetrics.recordOrderCreationFailure();
        return CompletableFuture.failedFuture(
            new RuntimeException("Order service temporarily unavailable. Please try again later.", exception)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#orderId")
    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "getOrderFallback")
    @Retry(name = "databaseRetry")
    @Bulkhead(name = "databaseBulkhead")
    @TimeLimiter(name = "databaseTimeout")
    public CompletableFuture<OrderResponse> getOrderById(UUID orderId) {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            
            // Retrieves order; maps to response; records metrics
            try {
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> OrderNotFoundException.withId(orderId.toString()));
                
                orderMetrics.recordOrderRetrieved();
                return mapToOrderResponse(order);
            } catch (OrderNotFoundException e) {
                orderMetrics.recordOrderNotFound();
                throw e;
            } finally {
                Duration duration = Duration.between(start, Instant.now());
                orderMetrics.recordOrderRetrieval(duration);
            }
        });
    }

    /**
     * Fallback method for getOrderById when circuit breaker is open
     */
    private CompletableFuture<OrderResponse> getOrderFallback(UUID orderId, Exception exception) {
        log.error("Failed to retrieve order {} - service unavailable", orderId, exception);
        return CompletableFuture.failedFuture(
            new RuntimeException("Order service temporarily unavailable. Please try again later.", exception)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orderList", key = "#userId + '_' + #pageable.pageNumber")
    @CircuitBreaker(name = "databaseCircuitBreaker")
    @Bulkhead(name = "databaseBulkhead")
    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orders", key = "#orderId"),
        @CacheEvict(value = "orderList", allEntries = true)
    })
    @CircuitBreaker(name = "databaseCircuitBreaker")
    @Retry(name = "databaseRetry")
    @Bulkhead(name = "databaseBulkhead")
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        log.info("Updating order {} status to {}", orderId, status);
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> OrderNotFoundException.withId(orderId.toString()));
            
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(status);
            orderRepository.save(order);
            
            // Record status update
            orderMetrics.recordOrderStatusUpdated();
            
            // Update state gauges
            updateStateMetrics(oldStatus, status);
            
            log.info("Order {} status updated to {}", orderId, status);
        } catch (OrderNotFoundException e) {
            orderMetrics.recordOrderNotFound();
            throw e;
        }
    }
    
    private void updateStateMetrics(OrderStatus oldStatus, OrderStatus newStatus) {
        // Decrement old state
        if (oldStatus == OrderStatus.CREATED) {
            orderMetrics.decrementCreatedOrders();
        } else if (oldStatus == OrderStatus.PAID) {
            orderMetrics.decrementPaidOrders();
        }
        
        // Increment new state
        if (newStatus == OrderStatus.CREATED) {
            orderMetrics.incrementCreatedOrders();
        } else if (newStatus == OrderStatus.PAID) {
            orderMetrics.incrementPaidOrders();
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .userEmail(order.getUserEmail())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderResponse.OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private List<OrderCreatedEvent.OrderItemEvent> mapItemsToEvent(Order order) {
        return order.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());
    }
}
