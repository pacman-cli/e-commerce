package com.ecommerce.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for Saga Execution
 * 
 * Provides dedicated thread pool for saga orchestration:
 * - Isolates saga execution from main application threads
 * - Prevents saga execution from blocking HTTP request handling
 * - Configurable pool size for different workloads
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * Executor for saga operations
     * - Core pool: 10 threads
     * - Max pool: 20 threads
     * - Queue: 100 tasks
     * - Prevents saga execution from blocking main threads
     */
    @Bean(name = "sagaExecutor")
    public Executor sagaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("saga-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
