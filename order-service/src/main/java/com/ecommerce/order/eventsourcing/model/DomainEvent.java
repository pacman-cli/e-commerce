package com.ecommerce.order.eventsourcing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event Entity for Event Sourcing
 * 
 * Stores all changes to aggregates as immutable events.
 * Events are the source of truth, current state is derived.
 * 
 * Event Structure:
 * - eventId: Unique event identifier
 * - aggregateId: ID of the aggregate (e.g., Order ID)
 * - aggregateType: Type of aggregate (e.g., "Order", "Payment")
 * - eventType: Business event type (e.g., "OrderCreated", "OrderPaid")
 * - eventVersion: Event schema version for evolution
 * - sequenceNumber: Order within aggregate (0, 1, 2, ...)
 * - payload: Event data (JSON)
 * - metadata: Additional context (correlation ID, user ID, etc.)
 * - occurredOn: When the event happened
 * 
 * Example:
 * ```
 * {
 *   "eventId": "550e8400-e29b-41d4-a716-446655440000",
 *   "aggregateId": "order-123",
 *   "aggregateType": "Order",
 *   "eventType": "OrderCreated",
 *   "eventVersion": 1,
 *   "sequenceNumber": 0,
 *   "payload": {
 *     "orderId": "order-123",
 *     "userId": "user-456",
 *     "items": [...],
 *     "totalAmount": 100.00
 *   },
 *   "metadata": {
 *     "correlationId": "abc-123",
 *     "userId": "user-456"
 *   },
 *   "occurredOn": "2024-01-15T10:30:00Z"
 * }
 * ```
 * 
 * Benefits:
 * - Complete audit trail
 * - Temporal queries ("What was state at time X?")
 * - Replay events to reconstruct state
 * - Event-driven architecture support
 */
@Entity
@Table(name = "domain_events", indexes = {
    @Index(name = "idx_aggregate", columnList = "aggregate_id, aggregate_type"),
    @Index(name = "idx_occurred_on", columnList = "occurred_on"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_correlation", columnList = "correlation_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String payload;

    @Lob
    @Column(name = "metadata", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String metadata;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "causation_id")
    private String causationId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Check if this event is a snapshot
     */
    @Transient
    public boolean isSnapshot() {
        return "Snapshot".equals(eventType);
    }

    /**
     * Get event age in milliseconds
     */
    @Transient
    public long getAgeInMillis() {
        return Instant.now().toEpochMilli() - occurredOn.toEpochMilli();
    }
}
