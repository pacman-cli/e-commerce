package com.ecommerce.order.eventsourcing.repository;

import com.ecommerce.order.eventsourcing.model.DomainEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Event Store Repository
 * 
 * Stores and retrieves domain events for event sourcing.
 * Events are append-only and immutable.
 * 
 * Query Capabilities:
 * - Load events by aggregate ID
 * - Load events by type and time range
 * - Get latest snapshot for aggregate
 * - Check for event existence
 */
@Repository
public interface EventStoreRepository extends JpaRepository<DomainEvent, String> {

    /**
     * Load all events for an aggregate, ordered by sequence
     */
    List<DomainEvent> findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(
            String aggregateId, String aggregateType);

    /**
     * Load events for aggregate from a specific sequence number
     * Used when loading from snapshot
     */
    List<DomainEvent> findByAggregateIdAndAggregateTypeAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            String aggregateId, String aggregateType, Long sequenceNumber);

    /**
     * Get the latest snapshot for an aggregate
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.aggregateId = :aggregateId " +
           "AND e.aggregateType = :aggregateType AND e.eventType = 'Snapshot' " +
           "ORDER BY e.sequenceNumber DESC")
    List<DomainEvent> findLatestSnapshot(
            @Param("aggregateId") String aggregateId,
            @Param("aggregateType") String aggregateType,
            Pageable pageable);

    /**
     * Get the maximum sequence number for an aggregate
     */
    @Query("SELECT MAX(e.sequenceNumber) FROM DomainEvent e " +
           "WHERE e.aggregateId = :aggregateId AND e.aggregateType = :aggregateType")
    Optional<Long> findMaxSequenceNumber(
            @Param("aggregateId") String aggregateId,
            @Param("aggregateType") String aggregateType);

    /**
     * Check if aggregate exists (has any events)
     */
    boolean existsByAggregateIdAndAggregateType(String aggregateId, String aggregateType);

    /**
     * Find events by type within time range
     * Useful for projections and analytics
     */
    List<DomainEvent> findByEventTypeAndOccurredOnBetween(
            String eventType, Instant start, Instant end);

    /**
     * Find events by correlation ID
     * Track related events across aggregates
     */
    List<DomainEvent> findByCorrelationIdOrderByOccurredOnAsc(String correlationId);

    /**
     * Count events for aggregate
     */
    long countByAggregateIdAndAggregateType(String aggregateId, String aggregateType);

    /**
     * Delete old events (for archiving)
     * Use with caution - breaks event sourcing guarantees
     */
    void deleteByOccurredOnBefore(Instant cutoff);
}
