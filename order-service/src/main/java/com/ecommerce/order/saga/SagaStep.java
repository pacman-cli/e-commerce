package com.ecommerce.order.saga;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Individual Step in a Saga
 * 
 * Each step has:
 * - Name: Identifier for the step
 * - Action: The operation to execute
 * - Compensation: The rollback operation
 * - Status: Current execution state
 * 
 * Example:
 * Step: Reserve Inventory
 * - Action: Call inventory service to reserve items
 * - Compensation: Release the reserved inventory
 * 
 * Retry Logic:
 * - Steps can be retried on transient failures
 * - Max retries configurable per step
 * - Exponential backoff between retries
 */
@Getter
@Setter
@Slf4j
public class SagaStep {
    
    private final String name;
    private final String description;
    private final Runnable action;
    private final Runnable compensation;
    private final int maxRetries;
    private final long retryDelayMs;
    
    private StepStatus status;
    private String errorMessage;
    private Instant executedAt;
    private Instant compensatedAt;
    private int retryCount;
    
    public enum StepStatus {
        PENDING,              // Not yet executed
        IN_PROGRESS,          // Currently executing
        COMPLETED,            // Successfully executed
        FAILED,               // Execution failed
        COMPENSATING,         // Compensation in progress
        COMPENSATED,          // Successfully compensated
        COMPENSATION_FAILED   // Compensation failed (requires manual intervention)
    }
    
    /**
     * Create a saga step without compensation (for steps that don't need rollback)
     */
    public SagaStep(String name, String description, Runnable action) {
        this(name, description, action, null, 3, 1000);
    }
    
    /**
     * Create a saga step with compensation
     */
    public SagaStep(String name, String description, Runnable action, Runnable compensation) {
        this(name, description, action, compensation, 3, 1000);
    }
    
    /**
     * Create a saga step with full configuration
     */
    public SagaStep(String name, String description, Runnable action, 
                    Runnable compensation, int maxRetries, long retryDelayMs) {
        this.name = name;
        this.description = description;
        this.action = action;
        this.compensation = compensation;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.status = StepStatus.PENDING;
        this.retryCount = 0;
    }
    
    /**
     * Execute the step with retry logic
     */
    public void execute() throws Exception {
        status = StepStatus.IN_PROGRESS;
        
        while (retryCount <= maxRetries) {
            try {
                action.run();
                status = StepStatus.COMPLETED;
                executedAt = Instant.now();
                log.info("Step '{}' executed successfully", name);
                return;
            } catch (Exception e) {
                retryCount++;
                log.warn("Step '{}' attempt {}/{} failed: {}", 
                    name, retryCount, maxRetries + 1, e.getMessage());
                
                if (retryCount > maxRetries) {
                    throw e;
                }
                
                // Wait before retry (exponential backoff)
                long delay = retryDelayMs * (1L << (retryCount - 1));
                log.debug("Waiting {}ms before retry", delay);
                Thread.sleep(delay);
            }
        }
    }
    
    /**
     * Execute compensation (rollback)
     */
    public void compensate() {
        if (compensation == null) {
            log.info("Step '{}' has no compensation action, skipping", name);
            status = StepStatus.COMPENSATED;
            return;
        }
        
        status = StepStatus.COMPENSATING;
        
        try {
            compensation.run();
            status = StepStatus.COMPENSATED;
            compensatedAt = Instant.now();
            log.info("Step '{}' compensated successfully", name);
        } catch (Exception e) {
            status = StepStatus.COMPENSATION_FAILED;
            log.error("Step '{}' compensation failed: {}", name, e.getMessage());
            throw new RuntimeException("Compensation failed for step: " + name, e);
        }
    }
    
    /**
     * Check if step has compensation
     */
    public boolean hasCompensation() {
        return compensation != null;
    }
    
    /**
     * Check if step can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    @Override
    public String toString() {
        return String.format("SagaStep{name=%s, status=%s, retries=%d/%d}",
            name, status, retryCount, maxRetries);
    }
    
    /**
     * Builder for fluent step creation
     */
    public static class Builder {
        private String name;
        private String description;
        private Runnable action;
        private Runnable compensation;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }
        
        public Builder compensation(Runnable compensation) {
            this.compensation = compensation;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }
        
        public SagaStep build() {
            if (name == null || action == null) {
                throw new IllegalStateException("Name and action are required");
            }
            return new SagaStep(name, description, action, compensation, maxRetries, retryDelayMs);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
