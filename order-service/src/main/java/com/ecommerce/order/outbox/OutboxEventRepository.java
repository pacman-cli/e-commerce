package com.ecommerce.order.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OutboxEvent entities.
 * Provides methods for querying and managing outbox events.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    /**
     * Find all unprocessed events ordered by creation time.
     * Used by the poller to get events to publish.
     */
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
    
    /**
     * Find unprocessed events with pagination.
     */
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);
    
    /**
     * Find events by aggregate type and ID.
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
    
    /**
     * Find specific event by aggregate details.
     */
    Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
        String aggregateType, 
        String aggregateId, 
        String eventType
    );
    
    /**
     * Count unprocessed events.
     */
    long countByProcessedFalse();
    
    /**
     * Count failed events (high retry count).
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.processed = false AND o.retryCount >= :maxRetries")
    long countFailedEvents(@Param("maxRetries") int maxRetries);
    
    /**
     * Delete old processed events.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.processed = true AND o.processedAt < :cutoffDate")
    int deleteOldProcessedEvents(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find events that have been stuck for a long time.
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.createdAt < :cutoffDate ORDER BY o.createdAt ASC")
    List<OutboxEvent> findStuckEvents(@Param("cutoffDate") Instant cutoffDate);
}
