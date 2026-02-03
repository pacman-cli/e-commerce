package com.ecommerce.order.config.resilience;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Bulkhead Configuration for Resource Isolation
 * 
 * The Bulkhead pattern limits concurrent calls to prevent resource exhaustion.
 * Two types of bulkheads:
 * 
 * 1. Semaphore Bulkhead - Limits concurrent calls within same thread
 * 2. ThreadPool Bulkhead - Uses separate thread pool for isolation
 * 
 * Configured Bulkheads:
 * - databaseBulkhead: Limits concurrent DB operations (20 max, 5 waiting)
 * - kafkaBulkhead: Limits concurrent Kafka operations (10 max)
 * - orderCreationBulkhead: Critical path isolation (50 max)
 * 
 * Benefits:
 * - Prevents cascading failures
 * - Isolates resources per operation type
 * - Allows graceful degradation under load
 */
@Configuration
@Slf4j
public class BulkheadConfiguration {

    /**
     * Semaphore bulkhead event consumer
     */
    @Bean
    public RegistryEventConsumer<BulkheadConfig> bulkheadEventConsumer() {
        // Logs semaphore bulkhead configuration registry events
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<BulkheadConfig> entryAddedEvent) {
                log.info("Bulkhead configuration '{}' added", 
                    entryAddedEvent.getAddedEntry().toString());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<BulkheadConfig> entryRemoveEvent) {
                log.info("Bulkhead configuration '{}' removed", 
                    entryRemoveEvent.getRemovedEntry().toString());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<BulkheadConfig> entryReplacedEvent) {
                log.info("Bulkhead configuration '{}' replaced", 
                    entryReplacedEvent.getNewEntry().toString());
            }
        };
    }

    /**
     * Thread pool bulkhead event consumer
     */
    @Bean
    public RegistryEventConsumer<ThreadPoolBulkheadConfig> threadPoolBulkheadEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<ThreadPoolBulkheadConfig> entryAddedEvent) {
                log.info("Thread Pool Bulkhead configuration '{}' added", 
                    entryAddedEvent.getAddedEntry().toString());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<ThreadPoolBulkheadConfig> entryRemoveEvent) {
                log.info("Thread Pool Bulkhead configuration '{}' removed", 
                    entryRemoveEvent.getRemovedEntry().toString());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<ThreadPoolBulkheadConfig> entryReplacedEvent) {
                log.info("Thread Pool Bulkhead configuration '{}' replaced", 
                    entryReplacedEvent.getNewEntry().toString());
            }
        };
    }

    /**
     * Database semaphore bulkhead
     * Limits concurrent database operations to prevent connection pool exhaustion
     * - Max concurrent: 20 (leaves headroom for other operations)
     * - Max wait: 500ms (fail fast if overloaded)
     */
    @Bean
    public BulkheadConfig databaseBulkheadConfig() {
        return BulkheadConfig.custom()
            // Maximum concurrent calls allowed (leave headroom for pool=30)
            .maxConcurrentCalls(20)
            // Max wait time for permission (fail fast if overloaded)
            .maxWaitDuration(Duration.ofMillis(500))
            // Writable stack trace (disable in production for performance)
            .writableStackTraceEnabled(true)
            .build();
    }

    /**
     * Kafka semaphore bulkhead
     * Limits concurrent Kafka operations
     * - Max concurrent: 10 (Kafka async, doesn't need many)
     * - Longer wait: 1 second (async operations can wait)
     */
    @Bean
    public BulkheadConfig kafkaBulkheadConfig() {
        return BulkheadConfig.custom()
            // Lower limit for async operations
            .maxConcurrentCalls(10)
            // Can wait longer for async operations
            .maxWaitDuration(Duration.ofSeconds(1))
            .writableStackTraceEnabled(true)
            .build();
    }

    /**
     * Order creation semaphore bulkhead (critical path)
     * High limit for important operations
     * - Max concurrent: 50
     * - Short wait: 100ms (fail fast, let client retry)
     */
    @Bean
    public BulkheadConfig orderCreationBulkheadConfig() {
        return BulkheadConfig.custom()
            // Higher limit for critical operations
            .maxConcurrentCalls(50)
            // Fail fast, let circuit breaker handle it
            .maxWaitDuration(Duration.ofMillis(100))
            .writableStackTraceEnabled(true)
            .build();
    }

    /**
     * Thread pool bulkhead for CPU-intensive operations
     * - Core thread pool: 10
     * - Max thread pool: 20
     * - Queue capacity: 100
     * - Keep alive: 20 seconds
     */
    @Bean
    public ThreadPoolBulkheadConfig cpuIntensiveBulkheadConfig() {
        return ThreadPoolBulkheadConfig.custom()
            // Core thread pool size
            .coreThreadPoolSize(10)
            // Maximum thread pool size
            .maxThreadPoolSize(20)
            // Queue capacity for pending tasks
            .queueCapacity(100)
            // Keep alive time for idle threads
            .keepAliveDuration(Duration.ofSeconds(20))
            .build();
    }

    /**
     * Semaphore Bulkhead Registry
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        
        // Add named configurations
        registry.addConfiguration("databaseBulkhead", databaseBulkheadConfig());
        registry.addConfiguration("kafkaBulkhead", kafkaBulkheadConfig());
        registry.addConfiguration("orderCreationBulkhead", orderCreationBulkheadConfig());
        
        log.info("Semaphore Bulkhead Registry initialized with {} configurations", 
            registry.getAllBulkheads().size());
        
        return registry;
    }

    /**
     * Thread Pool Bulkhead Registry
     */
    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
        ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.ofDefaults();
        
        // Add named configurations
        registry.addConfiguration("cpuIntensiveBulkhead", cpuIntensiveBulkheadConfig());
        
        log.info("Thread Pool Bulkhead Registry initialized with {} configurations", 
            registry.getAllBulkheads().size());
        
        return registry;
    }
}
