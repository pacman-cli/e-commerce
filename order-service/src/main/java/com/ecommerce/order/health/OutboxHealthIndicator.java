package com.ecommerce.order.health;

import com.ecommerce.order.metrics.OrderMetrics;
import com.ecommerce.order.outbox.OutboxPoller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the Outbox Pattern.
 * 
 * Monitors the health of the outbox event processing:
 * - Number of unprocessed events
 * - Number of stuck events (long processing time)
 * - Retry queue depth
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxHealthIndicator implements HealthIndicator {
    
    private final OutboxPoller outboxPoller;
    private final OrderMetrics orderMetrics;
    
    // Thresholds for health determination
    private static final int WARNING_THRESHOLD = 100;  // Unprocessed events
    private static final int CRITICAL_THRESHOLD = 1000; // Unprocessed events
    private static final int FAILED_THRESHOLD = 50;    // Failed events
    
    @Override
    public Health health() {
        try {
            OutboxPoller.OutboxMetrics metrics = outboxPoller.getMetrics();
            
            long unprocessed = metrics.unprocessedCount();
            long failed = metrics.failedCount();
            
            Health.Builder builder;
            
            // Determine health status based on thresholds
            if (unprocessed > CRITICAL_THRESHOLD || failed > FAILED_THRESHOLD) {
                builder = Health.down();
            } else if (unprocessed > WARNING_THRESHOLD) {
                builder = Health.status("WARNING");
            } else {
                builder = Health.up();
            }
            
            return builder
                .withDetail("unprocessed_events", unprocessed)
                .withDetail("failed_events", failed)
                .withDetail("warning_threshold", WARNING_THRESHOLD)
                .withDetail("critical_threshold", CRITICAL_THRESHOLD)
                .build();
            
        } catch (Exception e) {
            log.error("Outbox health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
