package com.ecommerce.user.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for outbox events.
 * 
 * Provides methods to query and manage outbox events for reliable
 * event publishing.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find unprocessed events ordered by creation time (FIFO).
     * Used by the poller to get events to publish.
     */
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);

    /**
     * Find a specific event by aggregate details.
     * Useful for idempotency checks.
     */
    Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
            String aggregateType, String aggregateId, String eventType);

    /**
     * Count unprocessed events.
     * Useful for monitoring and alerting.
     */
    long countByProcessedFalse();

    /**
     * Count events that have exceeded max retries.
     * Useful for DLQ monitoring.
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.retryCount >= ?1 AND e.processed = false")
    long countFailedEvents(int maxRetries);

    /**
     * Find events stuck for a long time (potential issues).
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.createdAt < ?1 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findStuckEvents(Instant cutoff);

    /**
     * Delete old processed events to prevent table bloat.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.createdAt < ?1")
    int deleteOldProcessedEvents(Instant cutoffDate);
}
