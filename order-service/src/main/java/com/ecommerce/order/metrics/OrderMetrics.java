package com.ecommerce.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business and infrastructure metrics for the Order Service.
 * 
 * Implements the "Four Golden Signals" of monitoring:
 * 1. Latency - How long it takes to service a request
 * 2. Traffic - How much demand is being placed on the system
 * 3. Errors - Rate of failed requests
 * 4. Saturation - How "full" the service is
 */
@Slf4j
@Component
public class OrderMetrics implements MeterBinder {
    
    private MeterRegistry meterRegistry;
    
    // Latency metrics
    private Timer orderCreationTimer;
    private Timer orderRetrievalTimer;
    
    // Traffic metrics (counters)
    private Counter ordersCreatedTotal;
    private Counter ordersRetrievedTotal;
    private Counter orderStatusUpdatesTotal;
    
    // Error metrics
    private Counter orderCreationFailures;
    private Counter orderNotFoundErrors;
    private Counter validationErrors;
    
    // Saturation metrics (gauges)
    private final AtomicInteger activeOrdersBeingProcessed = new AtomicInteger(0);
    private final AtomicLong totalOrderValue = new AtomicLong(0);
    private final AtomicInteger ordersInCreatedState = new AtomicInteger(0);
    private final AtomicInteger ordersInPaidState = new AtomicInteger(0);
    
    // Business metrics
    private Counter highValueOrders;
    private Counter ordersByCurrency;
    
    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;
        
        // Latency metrics
        this.orderCreationTimer = Timer.builder("orders.creation.duration")
            .description("Time taken to create an order")
            .publishPercentiles(0.5, 0.95, 0.99)
            .sla(Duration.ofMillis(100), Duration.ofMillis(500))
            .register(registry);
        
        this.orderRetrievalTimer = Timer.builder("orders.retrieval.duration")
            .description("Time taken to retrieve an order")
            .publishPercentiles(0.5, 0.95)
            .sla(Duration.ofMillis(50), Duration.ofMillis(200))
            .register(registry);
        
        // Traffic metrics
        this.ordersCreatedTotal = Counter.builder("orders.created.total")
            .description("Total number of orders created")
            .register(registry);
        
        this.ordersRetrievedTotal = Counter.builder("orders.retrieved.total")
            .description("Total number of orders retrieved")
            .register(registry);
        
        this.orderStatusUpdatesTotal = Counter.builder("orders.status.updates.total")
            .description("Total number of order status updates")
            .register(registry);
        
        // Error metrics
        this.orderCreationFailures = Counter.builder("orders.creation.failures")
            .description("Number of failed order creation attempts")
            .register(registry);
        
        this.orderNotFoundErrors = Counter.builder("orders.not_found.errors")
            .description("Number of order not found errors")
            .register(registry);
        
        this.validationErrors = Counter.builder("orders.validation.errors")
            .description("Number of validation errors")
            .register(registry);
        
        // Saturation metrics
        Gauge.builder("orders.active.processing", activeOrdersBeingProcessed, AtomicInteger::get)
            .description("Number of orders currently being processed")
            .register(registry);
        
        Gauge.builder("orders.total.value", totalOrderValue, AtomicLong::get)
            .description("Total value of all orders")
            .register(registry);
        
        Gauge.builder("orders.state.created", ordersInCreatedState, AtomicInteger::get)
            .description("Number of orders in CREATED state")
            .register(registry);
        
        Gauge.builder("orders.state.paid", ordersInPaidState, AtomicInteger::get)
            .description("Number of orders in PAID state")
            .register(registry);
        
        // Business metrics
        this.highValueOrders = Counter.builder("orders.high_value")
            .description("Number of high-value orders (>$1000)")
            .register(registry);
    }
    
    // Latency recording methods
    
    public void recordOrderCreation(Duration duration) {
        if (orderCreationTimer != null) {
            orderCreationTimer.record(duration);
        }
    }
    
    public void recordOrderRetrieval(Duration duration) {
        if (orderRetrievalTimer != null) {
            orderRetrievalTimer.record(duration);
        }
    }
    
    // Traffic recording methods
    
    public void recordOrderCreated() {
        if (ordersCreatedTotal != null) {
            ordersCreatedTotal.increment();
        }
    }
    
    public void recordOrderRetrieved() {
        if (ordersRetrievedTotal != null) {
            ordersRetrievedTotal.increment();
        }
    }
    
    public void recordOrderStatusUpdated() {
        if (orderStatusUpdatesTotal != null) {
            orderStatusUpdatesTotal.increment();
        }
    }
    
    // Error recording methods
    
    public void recordOrderCreationFailure() {
        if (orderCreationFailures != null) {
            orderCreationFailures.increment();
        }
    }
    
    public void recordOrderNotFound() {
        if (orderNotFoundErrors != null) {
            orderNotFoundErrors.increment();
        }
    }
    
    public void recordValidationError() {
        if (validationErrors != null) {
            validationErrors.increment();
        }
    }
    
    // Saturation update methods
    
    public void incrementActiveOrders() {
        activeOrdersBeingProcessed.incrementAndGet();
    }
    
    public void decrementActiveOrders() {
        activeOrdersBeingProcessed.decrementAndGet();
    }
    
    public void addToTotalValue(BigDecimal value) {
        totalOrderValue.addAndGet(value.longValue());
    }
    
    public void incrementCreatedOrders() {
        ordersInCreatedState.incrementAndGet();
    }
    
    public void decrementCreatedOrders() {
        ordersInCreatedState.decrementAndGet();
    }
    
    public void incrementPaidOrders() {
        ordersInPaidState.incrementAndGet();
    }
    
    public void decrementPaidOrders() {
        ordersInPaidState.decrementAndGet();
    }
    
    // Business metrics
    
    public void recordHighValueOrder(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            highValueOrders.increment();
        }
    }
    
    /**
     * Get current metrics snapshot for health checks.
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            activeOrdersBeingProcessed.get(),
            ordersInCreatedState.get(),
            ordersInPaidState.get()
        );
    }
    
    public record MetricsSnapshot(
        int activeOrders,
        int createdOrders,
        int paidOrders
    ) {}
}
