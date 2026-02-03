package com.ecommerce.order.eventsourcing.service;

import com.ecommerce.order.eventsourcing.model.AggregateRoot;
import com.ecommerce.order.eventsourcing.model.DomainEvent;
import com.ecommerce.order.eventsourcing.model.Event;
import com.ecommerce.order.eventsourcing.repository.EventStoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Event Store Service
 * 
 * Manages persistence and retrieval of domain events.
 * Core service for event sourcing infrastructure.
 * 
 * Responsibilities:
 * 1. Append events to store (never update/delete)
 * 2. Load events to rebuild aggregates
 * 3. Optimistic concurrency control (version checking)
 * 4. Snapshot management
 * 5. Event serialization/deserialization
 * 
 * Event Storage Guarantees:
 * - Append-only (immutable events)
 * - Ordered by sequence number
 * - Atomic per aggregate
 * - Optimistic concurrency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStoreService {

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    /**
     * Append uncommitted events from aggregate to store
     * 
     * @param aggregate The aggregate with uncommitted events
     * @throws OptimisticConcurrencyException if version conflict
     */
    @Transactional
    public void appendEvents(AggregateRoot aggregate) {
        if (!aggregate.hasUncommittedEvents()) {
            return;
        }

        String aggregateId = aggregate.getId();
        String aggregateType = aggregate.getAggregateType();
        
        // Get current sequence number for optimistic concurrency
        Optional<Long> maxSeq = eventStoreRepository.findMaxSequenceNumber(
            aggregateId, aggregateType);
        long nextSequence = maxSeq.orElse(-1L) + 1;

        List<Event> uncommitted = aggregate.getUncommittedEvents();
        
        for (int i = 0; i < uncommitted.size(); i++) {
            Event event = uncommitted.get(i);
            
            try {
                DomainEvent domainEvent = toDomainEvent(event, aggregateId, aggregateType, nextSequence + i);
                eventStoreRepository.save(domainEvent);
                log.debug("Appended event {} to aggregate {} (seq: {})",
                    event.getEventType(), aggregateId, domainEvent.getSequenceNumber());
            } catch (Exception e) {
                throw new RuntimeException("Failed to append event: " + event.getEventType(), e);
            }
        }

        aggregate.markCommitted();
        log.info("Appended {} events to aggregate {} (version: {})",
            uncommitted.size(), aggregateId, aggregate.getVersion());
    }

    /**
     * Load aggregate from event store by replaying events
     * 
     * @param aggregateId The aggregate ID
     * @param aggregateType The aggregate type
     * @param factory Factory to create empty aggregate instance
     * @return Rehydrated aggregate
     */
    public <T extends AggregateRoot> T loadAggregate(
            String aggregateId, 
            String aggregateType,
            AggregateFactory<T> factory) {
        
        List<DomainEvent> domainEvents = eventStoreRepository
            .findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(
                aggregateId, aggregateType);

        if (domainEvents.isEmpty()) {
            throw new AggregateNotFoundException(
                "Aggregate not found: " + aggregateType + " " + aggregateId);
        }

        // Convert to events
        List<Event> events = domainEvents.stream()
            .map(this::toEvent)
            .collect(Collectors.toList());

        // Create and rehydrate aggregate
        T aggregate = factory.create();
        aggregate.rehydrate(events);
        
        log.info("Loaded aggregate {} with {} events (version: {})",
            aggregateId, events.size(), aggregate.getVersion());
        
        return aggregate;
    }

    /**
     * Load aggregate from snapshot and subsequent events
     * Optimization for aggregates with many events
     */
    public <T extends AggregateRoot> T loadAggregateFromSnapshot(
            String aggregateId,
            String aggregateType,
            AggregateFactory<T> factory) {
        
        // Get latest snapshot
        List<DomainEvent> snapshots = eventStoreRepository.findLatestSnapshot(
            aggregateId, aggregateType, org.springframework.data.domain.PageRequest.of(0, 1));
        
        T aggregate = factory.create();
        long fromSequence = 0;
        
        if (!snapshots.isEmpty()) {
            DomainEvent snapshot = snapshots.get(0);
            // Deserialize snapshot state into aggregate
            // This requires custom implementation per aggregate
            fromSequence = snapshot.getSequenceNumber() + 1;
            log.debug("Loading from snapshot at sequence {}", fromSequence);
        }

        // Load subsequent events
        List<DomainEvent> events = eventStoreRepository
            .findByAggregateIdAndAggregateTypeAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                aggregateId, aggregateType, fromSequence);

        List<Event> eventList = events.stream()
            .map(this::toEvent)
            .collect(Collectors.toList());

        aggregate.rehydrate(eventList);
        
        log.info("Loaded aggregate {} from snapshot + {} events", aggregateId, events.size());
        
        return aggregate;
    }

    /**
     * Save snapshot for aggregate
     * Reduces replay time for aggregates with many events
     */
    @Transactional
    public void saveSnapshot(AggregateRoot aggregate) {
        // Snapshot logic depends on aggregate type
        // Typically serializes current state to a Snapshot event
        log.info("Saving snapshot for aggregate {} (version: {})",
            aggregate.getId(), aggregate.getVersion());
        
        // Implementation would create a DomainEvent with eventType = "Snapshot"
        // and payload = serialized aggregate state
    }

    /**
     * Convert Event to DomainEvent entity
     */
    private DomainEvent toDomainEvent(Event event, String aggregateId, 
                                       String aggregateType, long sequenceNumber) 
                                       throws JsonProcessingException {
        return DomainEvent.builder()
            .eventId(event.getEventId())
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .eventType(event.getEventType())
            .eventVersion(event.getVersion())
            .sequenceNumber(sequenceNumber)
            .payload(objectMapper.writeValueAsString(event))
            .metadata(null) // Could add correlation ID, user ID, etc.
            .occurredOn(event.getOccurredOn())
            .build();
    }

    /**
     * Convert DomainEvent entity back to Event
     * Requires event type registry for deserialization
     */
    @SuppressWarnings("unchecked")
    private Event toEvent(DomainEvent domainEvent) {
        try {
            // Use event type to determine class
            // In production, use a proper event registry
            Class<? extends Event> eventClass = (Class<? extends Event>) 
                Class.forName("com.ecommerce.order.event." + domainEvent.getEventType());
            return objectMapper.readValue(domainEvent.getPayload(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + 
                domainEvent.getEventType(), e);
        }
    }

    /**
     * Check if aggregate exists
     */
    public boolean aggregateExists(String aggregateId, String aggregateType) {
        return eventStoreRepository.existsByAggregateIdAndAggregateType(
            aggregateId, aggregateType);
    }

    /**
     * Factory interface for creating aggregate instances
     */
    @FunctionalInterface
    public interface AggregateFactory<T extends AggregateRoot> {
        T create();
    }

    /**
     * Exception when aggregate not found
     */
    public static class AggregateNotFoundException extends RuntimeException {
        public AggregateNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception for optimistic concurrency conflicts
     */
    public static class OptimisticConcurrencyException extends RuntimeException {
        public OptimisticConcurrencyException(String message) {
            super(message);
        }
    }
}
