package com.ecommerce.order.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Poller that reads events from the outbox table and publishes them to Kafka.
 * 
 * This runs as a background job, periodically checking for unprocessed events
 * and publishing them to Kafka. Once published successfully, the event is marked
 * as processed in the outbox table.
 * 
 * The poller handles:
 * - Reading unprocessed events
 * - Publishing to Kafka with topic derived from aggregate type
 * - Marking events as processed on success
 * - Retry logic with exponential backoff
 * - Dead letter queue for events that fail permanently
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {
    
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${outbox.poller.batch-size:100}")
    private int batchSize;
    
    @Value("${outbox.poller.max-retries:3}")
    private int maxRetries;
    
    @Value("${outbox.poller.delete-after-days:7}")
    private int deleteAfterDays;
    
    /**
     * Poll the outbox table every 100ms for new events.
     * This ensures low latency between event creation and publishing.
     */
    @Scheduled(fixedDelay = 100)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc(
            org.springframework.data.domain.Pageable.ofSize(batchSize)
        );
        
        if (events.isEmpty()) {
            return;
        }
        
        log.debug("Processing {} events from outbox", events.size());
        
        for (OutboxEvent event : events) {
            try {
                if (event.shouldRetry(maxRetries)) {
                    publishEvent(event);
                } else {
                    handleFailedEvent(event);
                }
            } catch (Exception e) {
                log.error("Error processing outbox event: {}", event.getId(), e);
                event.markFailed(e.getMessage());
                outboxRepository.save(event);
            }
        }
    }
    
    /**
     * Publish a single event to Kafka.
     */
    private void publishEvent(OutboxEvent event) {
        String topic = determineTopic(event);
        String key = event.getAggregateId();
        String payload = event.getPayload();
        
        // Build the producer record with headers
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        
        // Add headers for tracing and metadata
        if (event.getMetadata() != null) {
            record.headers().add("metadata", event.getMetadata().getBytes());
        }
        record.headers().add("eventType", event.getEventType().getBytes());
        record.headers().add("eventVersion", event.getEventVersion().getBytes());
        record.headers().add("outboxEventId", event.getId().toString().getBytes());
        
        // Send to Kafka and handle result
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} to Kafka: {}", 
                    event.getId(), ex.getMessage());
                event.markFailed(ex.getMessage());
                outboxRepository.save(event);
            } else {
                log.debug("Successfully published event {} to topic {} partition {} offset {}",
                    event.getId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                
                event.markProcessed();
                outboxRepository.save(event);
            }
        });
    }
    
    /**
     * Determine the Kafka topic from the aggregate type.
     */
    private String determineTopic(OutboxEvent event) {
        // Convert aggregate type to topic name
        // e.g., "Order" -> "order-events"
        return event.getAggregateType().toLowerCase() + "-events";
    }
    
    /**
     * Handle events that have exceeded the retry limit.
     * These go to a dead letter queue for manual investigation.
     */
    private void handleFailedEvent(OutboxEvent event) {
        log.error("Event {} exceeded max retries ({}). Sending to DLQ.", 
            event.getId(), maxRetries);
        
        // Send to dead letter topic
        String dlqTopic = determineTopic(event) + ".dlq";
        
        try {
            kafkaTemplate.send(dlqTopic, event.getAggregateId(), event.getPayload());
            
            // Mark as processed to prevent infinite retries
            // (it's now in the DLQ)
            event.markProcessed();
            outboxRepository.save(event);
            
        } catch (Exception e) {
            log.error("Failed to send event {} to DLQ: {}", event.getId(), e.getMessage());
            // Keep it unprocessed so it can be retried later
        }
    }
    
    /**
     * Clean up old processed events daily.
     * Keeps the outbox table from growing indefinitely.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoffDate = Instant.now().minus(deleteAfterDays, ChronoUnit.DAYS);
        
        int deleted = outboxRepository.deleteOldProcessedEvents(cutoffDate);
        
        if (deleted > 0) {
            log.info("Cleaned up {} old processed outbox events", deleted);
        }
    }
    
    /**
     * Alert on stuck events (events that haven't been processed for a long time).
     * Run every 15 minutes.
     */
    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void alertOnStuckEvents() {
        Instant cutoffDate = Instant.now().minus(1, ChronoUnit.HOURS);
        List<OutboxEvent> stuckEvents = outboxRepository.findStuckEvents(cutoffDate);
        
        if (!stuckEvents.isEmpty()) {
            log.error("ALERT: {} events have been stuck for over 1 hour", stuckEvents.size());
            
            for (OutboxEvent event : stuckEvents) {
                log.error("Stuck event: {} - {} - {} - Retry count: {}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getRetryCount());
            }
            
            // Here you would send an alert to PagerDuty/Slack
        }
    }
    
    /**
     * Get metrics about the outbox.
     */
    public OutboxMetrics getMetrics() {
        long unprocessed = outboxRepository.countByProcessedFalse();
        long failed = outboxRepository.countFailedEvents(maxRetries);
        
        return new OutboxMetrics(unprocessed, failed);
    }
    
    /**
     * Metrics data class.
     */
    public record OutboxMetrics(long unprocessedCount, long failedCount) {}
}
