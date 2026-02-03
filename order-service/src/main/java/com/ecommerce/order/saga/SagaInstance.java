package com.ecommerce.order.saga;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saga Orchestrator for Distributed Transactions
 * 
 * The Saga pattern manages long-running transactions across multiple services
 * by breaking them into a sequence of local transactions.
 * 
 * Two types of sagas:
 * 1. Orchestration (this implementation) - Central coordinator manages steps
 * 2. Choreography - Services react to events without central coordinator
 * 
 * Compensation:
 * Each step has a compensating action that reverses its effect if the saga fails.
 * 
 * Example Order Processing Saga:
 * 1. Reserve Inventory (compensate: Release Inventory)
 * 2. Process Payment (compensate: Refund Payment)
 * 3. Create Order (compensate: Cancel Order)
 * 4. Send Notification (no compensation needed)
 * 
 * Benefits:
 * - Maintains data consistency across services
 * - No distributed locks (better performance)
 * - Isolates failures
 * - Supports long-running operations
 */
@Getter
@Setter
@Slf4j
public class SagaInstance {
    
    private final UUID sagaId;
    private final String sagaType;
    private final List<SagaStep> steps;
    private SagaStatus status;
    private int currentStepIndex;
    private final Instant createdAt;
    private Instant updatedAt;
    private String failureReason;
    
    public enum SagaStatus {
        STARTED,        // Saga initiated
        IN_PROGRESS,    // Executing steps
        COMPLETED,      // All steps successful
        COMPENSATING,   // Executing compensations
        COMPENSATED,    // All compensations successful
        FAILED          // Saga failed, compensations completed
    }
    
    public SagaInstance(String sagaType, List<SagaStep> steps) {
        this.sagaId = UUID.randomUUID();
        this.sagaType = sagaType;
        this.steps = new ArrayList<>(steps);
        this.status = SagaStatus.STARTED;
        this.currentStepIndex = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Execute the next step in the saga
     */
    public boolean executeNextStep() {
        if (currentStepIndex >= steps.size()) {
            status = SagaStatus.COMPLETED;
            updatedAt = Instant.now();
            log.info("Saga {} completed successfully", sagaId);
            return false;
        }
        
        SagaStep currentStep = steps.get(currentStepIndex);
        status = SagaStatus.IN_PROGRESS;
        
        try {
            log.info("Executing saga step {}/{}: {}", 
                currentStepIndex + 1, steps.size(), currentStep.getName());
            
            currentStep.execute();
            currentStep.setStatus(SagaStep.StepStatus.COMPLETED);
            currentStepIndex++;
            updatedAt = Instant.now();
            
            return true;
        } catch (Exception e) {
            log.error("Saga step {} failed: {}", currentStep.getName(), e.getMessage());
            currentStep.setStatus(SagaStep.StepStatus.FAILED);
            currentStep.setErrorMessage(e.getMessage());
            failureReason = e.getMessage();
            
            // Start compensation
            startCompensation();
            return false;
        }
    }
    
    /**
     * Start compensating (rolling back) completed steps
     */
    public void startCompensation() {
        status = SagaStatus.COMPENSATING;
        log.info("Starting compensation for saga {}", sagaId);
        
        // Compensate in reverse order
        for (int i = currentStepIndex - 1; i >= 0; i--) {
            SagaStep step = steps.get(i);
            if (step.getStatus() == SagaStep.StepStatus.COMPLETED) {
                try {
                    log.info("Compensating step: {}", step.getName());
                    step.compensate();
                    step.setStatus(SagaStep.StepStatus.COMPENSATED);
                } catch (Exception e) {
                    log.error("Compensation failed for step {}: {}", 
                        step.getName(), e.getMessage());
                    step.setStatus(SagaStep.StepStatus.COMPENSATION_FAILED);
                    // Log for manual intervention
                }
            }
        }
        
        status = status == SagaStatus.COMPENSATING ? SagaStatus.COMPENSATED : SagaStatus.FAILED;
        updatedAt = Instant.now();
        log.info("Saga {} compensation completed with status: {}", sagaId, status);
    }
    
    /**
     * Get current step
     */
    public SagaStep getCurrentStep() {
        if (currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }
    
    /**
     * Check if saga is complete (success or failure)
     */
    public boolean isComplete() {
        return status == SagaStatus.COMPLETED || 
               status == SagaStatus.FAILED || 
               status == SagaStatus.COMPENSATED;
    }
    
    /**
     * Check if saga completed successfully
     */
    public boolean isSuccessful() {
        return status == SagaStatus.COMPLETED;
    }
    
    @Override
    public String toString() {
        return String.format("SagaInstance{id=%s, type=%s, status=%s, step=%d/%d}",
            sagaId, sagaType, status, currentStepIndex, steps.size());
    }
}
