package com.ecommerce.order.config.resilience;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Time Limiter (Timeout) Configuration
 * 
 * Prevents operations from hanging indefinitely:
 * 1. databaseTimeout - 3 seconds for DB operations
 * 2. kafkaTimeout - 5 seconds for Kafka operations
 * 3. externalServiceTimeout - 3 seconds for external calls
 * 4. orderCreationTimeout - 5 seconds for critical order creation
 * 
 * When timeout is exceeded:
 * - Future is cancelled
 * - Circuit breaker may open
 * - Fallback can be triggered
 * 
 * Configuration:
 * - timeoutDuration: Maximum allowed execution time
 * - cancelRunningFuture: Cancel the task when timeout occurs
 */
@Configuration
@Slf4j
public class TimeoutConfiguration {

    /**
     * Time limiter event consumer
     */
    @Bean
    public RegistryEventConsumer<TimeLimiterConfig> timeLimiterEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<TimeLimiterConfig> entryAddedEvent) {
                log.info("Time Limiter configuration '{}' added", 
                    entryAddedEvent.getAddedEntry().toString());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<TimeLimiterConfig> entryRemoveEvent) {
                log.info("Time Limiter configuration '{}' removed", 
                    entryRemoveEvent.getRemovedEntry().toString());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<TimeLimiterConfig> entryReplacedEvent) {
                log.info("Time Limiter configuration '{}' replaced", 
                    entryReplacedEvent.getNewEntry().toString());
            }
        };
    }

    /**
     * Database timeout configuration
     * - 3 seconds for most DB operations
     * - Cancel running query to free connection
     */
    @Bean
    public TimeLimiterConfig databaseTimeoutConfig() {
        return TimeLimiterConfig.custom()
            // Maximum execution time for database operations
            .timeoutDuration(Duration.ofSeconds(3))
            // Cancel the running query when timeout occurs (release connection)
            .cancelRunningFuture(true)
            .build();
    }

    /**
     * Kafka timeout configuration
     * - 5 seconds for Kafka operations
     * - Async operations can take longer
     */
    @Bean
    public TimeLimiterConfig kafkaTimeoutConfig() {
        return TimeLimiterConfig.custom()
            // Longer timeout for async Kafka operations
            .timeoutDuration(Duration.ofSeconds(5))
            // Cancel the future to prevent resource leak
            .cancelRunningFuture(true)
            .build();
    }

    /**
     * External service timeout configuration
     * - 3 seconds for external HTTP calls
     * - Fail fast, don't wait for slow external services
     */
    @Bean
    public TimeLimiterConfig externalServiceTimeoutConfig() {
        return TimeLimiterConfig.custom()
            // Fail fast for external services
            .timeoutDuration(Duration.ofSeconds(3))
            // Cancel the HTTP request
            .cancelRunningFuture(true)
            .build();
    }

    /**
     * Order creation timeout configuration
     * - 5 seconds for order creation (critical path)
     * - Slightly longer for complex transactions
     */
    @Bean
    public TimeLimiterConfig orderCreationTimeoutConfig() {
        return TimeLimiterConfig.custom()
            // Longer timeout for critical order operations
            .timeoutDuration(Duration.ofSeconds(5))
            // Cancel the transaction if taking too long
            .cancelRunningFuture(true)
            .build();
    }

    /**
     * Query timeout configuration (for complex queries)
     * - 10 seconds for heavy queries
     * - Used for reporting/analytics queries
     */
    @Bean
    public TimeLimiterConfig queryTimeoutConfig() {
        return TimeLimiterConfig.custom()
            // Longer timeout for complex queries
            .timeoutDuration(Duration.ofSeconds(10))
            // Cancel the query
            .cancelRunningFuture(true)
            .build();
    }

    /**
     * Time Limiter Registry
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        
        // Add named configurations
        registry.addConfiguration("databaseTimeout", databaseTimeoutConfig());
        registry.addConfiguration("kafkaTimeout", kafkaTimeoutConfig());
        registry.addConfiguration("externalServiceTimeout", externalServiceTimeoutConfig());
        registry.addConfiguration("orderCreationTimeout", orderCreationTimeoutConfig());
        registry.addConfiguration("queryTimeout", queryTimeoutConfig());
        
        log.info("Time Limiter Registry initialized with {} configurations", 
            registry.getAllTimeLimiters().size());
        
        return registry;
    }
}
