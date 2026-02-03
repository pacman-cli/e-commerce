package com.ecommerce.order.config.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration for Order Service
 * 
 * Protects against cascading failures when dependent services are down.
 * Three circuit breakers configured:
 * 1. databaseCircuitBreaker - For database operations
 * 2. kafkaCircuitBreaker - For Kafka messaging
 * 3. externalServiceCircuitBreaker - For external HTTP calls
 * 
 * Key Parameters:
 * - failureRateThreshold: 50% (open circuit after 50% failures)
 * - slowCallRateThreshold: 80% (consider slow calls as failures)
 * - slowCallDurationThreshold: 2 seconds
 * - waitDurationInOpenState: 30 seconds (cooling period)
 * - permittedNumberOfCallsInHalfOpenState: 10 (test calls before closing)
 * - slidingWindowSize: 100 (number of calls to track)
 */
@Configuration
@Slf4j
public class CircuitBreakerConfiguration {

    /**
     * Custom registry event consumer to log circuit breaker state changes
     */
    @Bean
    public RegistryEventConsumer<CircuitBreakerConfig> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreakerConfig> entryAddedEvent) {
                log.info("Circuit Breaker '{}' added to registry", 
                    entryAddedEvent.getAddedEntry().toString());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreakerConfig> entryRemoveEvent) {
                log.info("Circuit Breaker '{}' removed from registry", 
                    entryRemoveEvent.getRemovedEntry().toString());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreakerConfig> entryReplacedEvent) {
                log.info("Circuit Breaker '{}' replaced in registry", 
                    entryReplacedEvent.getNewEntry().toString());
            }
        };
    }

    /**
     * Circuit Breaker configuration for database operations
     * Opens after 50% failure rate with 2-second timeout
     */
    @Bean
    public CircuitBreakerConfig databaseCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            // Failure rate threshold - open circuit if 50% of calls fail
            .failureRateThreshold(50)
            // Slow call rate threshold - consider slow calls as failures if 80% are slow
            .slowCallRateThreshold(80)
            // Slow call duration threshold - calls taking > 2 seconds are considered slow
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            // Wait duration in open state - try again after 30 seconds
            .waitDurationInOpenState(Duration.ofSeconds(30))
            // Permitted number of calls in half-open state - test with 10 calls
            .permittedNumberOfCallsInHalfOpenState(10)
            // Sliding window size - track last 100 calls for failure rate calculation
            .slidingWindowSize(100)
            // Minimum number of calls required before calculating failure rate
            .minimumNumberOfCalls(20)
            // Automatic transition from open to half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            // Record exceptions that should count as failures
            .recordExceptions(
                java.sql.SQLException.class,
                org.springframework.dao.DataAccessException.class,
                java.util.concurrent.TimeoutException.class
            )
            // Ignore certain exceptions (business logic errors, not infrastructure)
            .ignoreExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class
            )
            .build();
    }

    /**
     * Circuit Breaker configuration for Kafka operations
     * More lenient due to async nature, longer timeout
     */
    @Bean
    public CircuitBreakerConfig kafkaCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            // Higher failure threshold for async operations
            .failureRateThreshold(60)
            // Lower slow call threshold since Kafka is async
            .slowCallRateThreshold(70)
            // Longer timeout for Kafka (5 seconds)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            // Longer cooling period for Kafka (60 seconds)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            // Test with 5 calls in half-open state
            .permittedNumberOfCallsInHalfOpenState(5)
            // Larger sliding window for async operations
            .slidingWindowSize(50)
            .minimumNumberOfCalls(10)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                org.apache.kafka.common.errors.TimeoutException.class,
                org.apache.kafka.common.errors.NotLeaderOrFollowerException.class,
                org.springframework.kafka.KafkaException.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class
            )
            .build();
    }

    /**
     * Circuit Breaker configuration for external service calls
     * Strict thresholds for external dependencies
     */
    @Bean
    public CircuitBreakerConfig externalServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            // Strict failure threshold for external services
            .failureRateThreshold(40)
            // High slow call rate threshold
            .slowCallRateThreshold(90)
            // Quick timeout for external calls (3 seconds)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            // Short cooling period (20 seconds)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            // Test with 5 calls
            .permittedNumberOfCallsInHalfOpenState(5)
            .slidingWindowSize(50)
            .minimumNumberOfCalls(10)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.net.SocketTimeoutException.class,
                java.net.ConnectException.class,
                org.springframework.web.client.ResourceAccessException.class,
                java.util.concurrent.TimeoutException.class
            )
            .ignoreExceptions(
                java.net.UnknownHostException.class // Don't count DNS issues
            )
            .build();
    }

    /**
     * Circuit Breaker Registry with all configurations
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Add named configurations
        registry.addConfiguration("databaseCircuitBreaker", databaseCircuitBreakerConfig());
        registry.addConfiguration("kafkaCircuitBreaker", kafkaCircuitBreakerConfig());
        registry.addConfiguration("externalServiceCircuitBreaker", externalServiceCircuitBreakerConfig());
        
        log.info("Circuit Breaker Registry initialized with {} configurations", 
            registry.getAllCircuitBreakers().size());
        
        return registry;
    }
}
