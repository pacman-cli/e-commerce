package com.ecommerce.order.config.resilience;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Retry Configuration with Exponential Backoff
 * 
 * Implements intelligent retry strategies for transient failures:
 * 1. databaseRetry - For database connection issues
 * 2. kafkaRetry - For Kafka message delivery
 * 3. externalServiceRetry - For external HTTP calls
 * 
 * Exponential backoff prevents overwhelming failing services:
 * - Initial wait: 100ms
 * - Multiplier: 2.0 (doubles each attempt)
 * - Max wait: 10 seconds
 * - Max attempts: 3-5 depending on criticality
 * 
 * Example backoff sequence (databaseRetry):
 * - Attempt 1: Wait 100ms
 * - Attempt 2: Wait 200ms (100ms * 2)
 * - Attempt 3: Wait 400ms (200ms * 2)
 * - Attempt 4: Wait 800ms (capped at 10s max)
 * - Attempt 5: Wait 1000ms
 */
@Configuration
@Slf4j
public class RetryConfiguration {

    /**
     * Custom registry event consumer to log retry state changes
     */
    @Bean
    public RegistryEventConsumer<RetryConfig> retryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<RetryConfig> entryAddedEvent) {
                log.info("Retry configuration '{}' added to registry", 
                    entryAddedEvent.getAddedEntry().toString());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<RetryConfig> entryRemoveEvent) {
                log.info("Retry configuration '{}' removed from registry", 
                    entryRemoveEvent.getRemovedEntry().toString());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<RetryConfig> entryReplacedEvent) {
                log.info("Retry configuration '{}' replaced in registry", 
                    entryReplacedEvent.getNewEntry().toString());
            }
        };
    }

    /**
     * Database retry configuration
     * - 3 attempts for database operations
     * - Exponential backoff starting at 100ms
     * - Only retry on connection/timeout errors
     */
    @Bean
    public RetryConfig databaseRetryConfig() {
        return RetryConfig.<Throwable>custom()
            // Maximum number of retry attempts
            .maxAttempts(3)
            // Exponential backoff with initial 100ms, multiplier 2.0, max 10s
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofMillis(100),
                2.0,
                Duration.ofSeconds(10)
            ))
            // Retry on specific exceptions (transient failures)
            .retryExceptions(
                java.sql.SQLException.class,
                org.springframework.dao.TransientDataAccessResourceException.class,
                org.springframework.transaction.CannotCreateTransactionException.class,
                java.util.concurrent.TimeoutException.class
            )
            // Don't retry on these (permanent failures)
            .ignoreExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class,
                org.springframework.dao.DataIntegrityViolationException.class
            )
            .build();
    }

    /**
     * Kafka retry configuration
     * - 5 attempts for critical message delivery
     * - Longer initial wait (500ms) due to async nature
     * - Higher max wait (30 seconds) for broker recovery
     */
    @Bean
    public RetryConfig kafkaRetryConfig() {
        return RetryConfig.<Throwable>custom()
            // More attempts for critical async operations
            .maxAttempts(5)
            // Exponential backoff with initial 500ms, multiplier 2.0, max 30s
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofMillis(500),
                2.0,
                Duration.ofSeconds(30)
            ))
            // Retry on Kafka-specific exceptions
            .retryExceptions(
                org.apache.kafka.common.errors.TimeoutException.class,
                org.apache.kafka.common.errors.NotLeaderOrFollowerException.class,
                org.apache.kafka.common.errors.NotEnoughReplicasException.class,
                org.springframework.kafka.KafkaException.class
            )
            // Don't retry on these
            .ignoreExceptions(
                IllegalArgumentException.class,
                org.apache.kafka.common.errors.SerializationException.class
            )
            .build();
    }

    /**
     * External service retry configuration
     * - 3 attempts for external HTTP calls
     * - Quick initial retry (100ms)
     - Cap at 5 seconds (external services should be fast)
     */
    @Bean
    public RetryConfig externalServiceRetryConfig() {
        return RetryConfig.<Throwable>custom()
            // Standard 3 attempts for external calls
            .maxAttempts(3)
            // Exponential backoff with initial 100ms, multiplier 2.0, max 5s
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofMillis(100),
                2.0,
                Duration.ofSeconds(5)
            ))
            // Retry on network/timeout exceptions
            .retryExceptions(
                java.net.SocketTimeoutException.class,
                java.net.ConnectException.class,
                java.net.SocketException.class,
                org.springframework.web.client.ResourceAccessException.class,
                java.util.concurrent.TimeoutException.class
            )
            // Don't retry on 4xx errors (client errors)
            .ignoreExceptions(
                IllegalArgumentException.class,
                org.springframework.web.client.HttpClientErrorException.class
            )
            .build();
    }

    /**
     * Outbox retry configuration (for Outbox Pattern)
     * - 10 attempts for guaranteed delivery
     * - Longer backoff to avoid overwhelming Kafka
     * - Used by OutboxPoller for reliable event publishing
     */
    @Bean
    public RetryConfig outboxRetryConfig() {
        return RetryConfig.<Throwable>custom()
            // Many attempts for guaranteed delivery
            .maxAttempts(10)
            // Exponential backoff with initial 1s, multiplier 2.5, max 5min
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofSeconds(1),
                2.5,
                Duration.ofMinutes(5)
            ))
            // Retry on all publishing errors
            .retryExceptions(
                org.apache.kafka.common.errors.TimeoutException.class,
                org.apache.kafka.common.errors.NotLeaderOrFollowerException.class,
                org.springframework.kafka.KafkaException.class,
                java.util.concurrent.TimeoutException.class
            )
            // Only permanent failures stop retry
            .ignoreExceptions(
                IllegalArgumentException.class,
                org.apache.kafka.common.errors.SerializationException.class
            )
            .build();
    }

    /**
     * Retry Registry with all configurations
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        
        // Add named configurations
        registry.addConfiguration("databaseRetry", databaseRetryConfig());
        registry.addConfiguration("kafkaRetry", kafkaRetryConfig());
        registry.addConfiguration("externalServiceRetry", externalServiceRetryConfig());
        registry.addConfiguration("outboxRetry", outboxRetryConfig());
        
        log.info("Retry Registry initialized with {} configurations", 
            registry.getAllRetries().size());
        
        return registry;
    }
}
