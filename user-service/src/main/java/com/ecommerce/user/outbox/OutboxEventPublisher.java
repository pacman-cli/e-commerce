package com.ecommerce.user.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event publisher that uses the Outbox Pattern for reliable event publishing.
 * 
 * Instead of publishing directly to Kafka, this publisher writes events to an
 * outbox table in the same database transaction as the business operation.
 * A separate poller then reads from the outbox and publishes to Kafka.
 * 
 * This ensures atomicity: either both the business data and the event are saved,
 * or neither is saved. No more orphaned records or lost events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Publish an event using the Outbox Pattern.
     * 
     * This method must be called within a transactional context.
     * The event will be saved to the outbox table in the same transaction
     * as the business operation.
     * 
     * @param aggregateType The type of aggregate (e.g., "User")
     * @param aggregateId The ID of the aggregate
     * @param eventType The type of event (e.g., "UserCreated")
     * @param payload The event payload (will be serialized to JSON)
     * @return The created outbox event
     */
    @Transactional
    public OutboxEvent publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        return publish(aggregateType, aggregateId, eventType, "1.0", payload, null);
    }
    
    /**
     * Publish an event with version and metadata.
     */
    @Transactional
    public OutboxEvent publish(
            String aggregateType, 
            String aggregateId, 
            String eventType,
            String eventVersion,
            Object payload,
            Map<String, String> metadata) {
        
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String metadataJson = metadata != null ? objectMapper.writeValueAsString(metadata) : null;
            
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .eventVersion(eventVersion)
                .payload(payloadJson)
                .metadata(metadataJson)
                .processed(false)
                .retryCount(0)
                .build();
            
            OutboxEvent saved = outboxRepository.save(event);
            
            log.debug("Event saved to outbox: {} - {} - {}", 
                aggregateType, eventType, aggregateId);
            
            return saved;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload: {}", payload, e);
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }
    
    /**
     * Publish an event with correlation ID for tracing.
     */
    @Transactional
    public OutboxEvent publishWithCorrelation(
            String aggregateType,
            String aggregateId, 
            String eventType,
            Object payload,
            String correlationId) {
        
        Map<String, String> metadata = Map.of(
            "correlationId", correlationId,
            "timestamp", Instant.now().toString()
        );
        
        return publish(aggregateType, aggregateId, eventType, "1.0", payload, metadata);
    }
    
    /**
     * Exception thrown when event serialization fails.
     */
    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
