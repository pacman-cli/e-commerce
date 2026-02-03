package com.ecommerce.order.eventsourcing.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events
 * 
 * Domain events represent business occurrences that have already happened.
 * They are immutable facts about the domain.
 * 
 * Characteristics:
 * - Immutable (never change after creation)
 * - Named in past tense (OrderCreated, PaymentProcessed)
 * - Contain all data needed to understand the event
 * - Self-contained (no references to other objects)
 * 
 * Usage:
 * ```java
 * public class OrderCreatedEvent extends Event {
 *     private final String orderId;
 *     private final String userId;
 *     private final BigDecimal total;
 *     
 *     public OrderCreatedEvent(String orderId, String userId, BigDecimal total) {
 *         super();
 *         this.orderId = orderId;
 *         this.userId = userId;
 *         this.total = total;
 *     }
 * }
 * ```
 */
@Getter
public abstract class Event {
    
    private final UUID eventId;
    private final Instant occurredOn;
    private final int version;
    
    protected Event() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.version = 1;
    }
    
    protected Event(UUID eventId, Instant occurredOn, int version) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.version = version;
    }
    
    /**
     * Get the event type name
     * Override to provide custom naming
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Get the aggregate ID this event belongs to
     * Must be implemented by concrete events
     */
    public abstract String getAggregateId();
    
    /**
     * Get the aggregate type
     */
    public abstract String getAggregateType();
    
    @Override
    public String toString() {
        return String.format("%s{id=%s, aggregate=%s, occurred=%s}",
            getEventType(), eventId, getAggregateId(), occurredOn);
    }
}
