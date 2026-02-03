package com.ecommerce.payment.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity for tracking idempotent requests.
 * 
 * Prevents duplicate processing of the same request by storing
 * the idempotency key and the response. Subsequent requests with
 * the same key return the stored response instead of reprocessing.
 */
@Entity
@Table(name = "idempotent_requests", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_idempotency_expires", columnList = "expiresAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String idempotencyKey;
    
    @Column(nullable = false)
    private String requestHash;
    
    @Column(nullable = false, length = 50)
    private String requestType;
    
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String responsePayload;
    
    @Column(nullable = false)
    private int responseStatusCode;
    
    @Column(nullable = false)
    private Instant expiresAt;
    
    @CreationTimestamp
    private Instant createdAt;
    
    /**
     * Check if this idempotent request has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Builder with default expiration of 24 hours.
     */
    public static class IdempotentRequestBuilder {
        private Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
    }
}
