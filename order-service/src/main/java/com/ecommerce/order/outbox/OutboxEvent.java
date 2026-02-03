package com.ecommerce.order.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox event entity for transactional event publishing.
 * 
 * The Outbox Pattern ensures that database changes and event publishing
 * are atomic - either both succeed or both fail. This prevents the
 * dual-write problem where a DB transaction commits but the event fails to publish.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_processed", columnList = "processed"),
    @Index(name = "idx_outbox_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String aggregateType;
    
    @Column(nullable = false, length = 255)
    private String aggregateId;
    
    @Column(nullable = false, length = 255)
    private String eventType;
    
    @Column(nullable = false, length = 50)
    private String eventVersion;
    
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean processed = false;
    
    @Column
    private Instant processedAt;
    
    @Column(length = 500)
    private String errorMessage;
    
    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    /**
     * Marks the event as successfully processed.
     */
    public void markProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }
    
    /**
     * Marks the event as failed with an error message.
     */
    public void markFailed(String error) {
        this.errorMessage = error;
        this.retryCount++;
    }
    
    /**
     * Checks if the event should be retried.
     */
    public boolean shouldRetry(int maxRetries) {
        return !processed && retryCount < maxRetries;
    }
}
