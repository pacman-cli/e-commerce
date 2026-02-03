package com.ecommerce.payment.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for IdempotentRequest entities.
 */
@Repository
public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, UUID> {
    
    /**
     * Find an idempotent request by its key.
     */
    Optional<IdempotentRequest> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Delete expired requests.
     */
    @Modifying
    @Query("DELETE FROM IdempotentRequest i WHERE i.expiresAt < :now")
    int deleteExpiredRequests(@Param("now") Instant now);
    
    /**
     * Check if a key exists.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
