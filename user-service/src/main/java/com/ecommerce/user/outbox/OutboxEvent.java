package com.ecommerce.user.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox event entity for reliable event publishing.
 * 
 * The outbox pattern ensures that events are persisted in the same
 * database transaction as business data changes, eliminating the dual-write problem.
 * A separate poller then reads from this table and publishes to Kafka.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_processed", columnList = "processed, created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false, length = 10)
    @Builder.Default
    private String eventVersion = "1.0";

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "processed_at")
    private Instant processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Check if this event should be retried based on max retry count.
     */
    public boolean shouldRetry(int maxRetries) {
        return retryCount < maxRetries;
    }

    /**
     * Mark this event as successfully processed.
     */
    public void markProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * Mark this event as failed and increment retry count.
     */
    public void markFailed(String error) {
        this.retryCount++;
        this.errorMessage = error;
    }
}
