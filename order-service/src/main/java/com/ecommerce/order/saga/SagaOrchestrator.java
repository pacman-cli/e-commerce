package com.ecommerce.order.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga Orchestrator Service
 * 
 * Central coordinator for executing sagas. Manages saga lifecycle:
 * 1. Create saga instance
 * 2. Execute steps sequentially
 * 3. Handle failures with compensation
 * 4. Track saga state
 * 
 * Persistent Storage:
 * In production, saga state should be persisted to database for:
 * - Recovery after service restart
 * - Monitoring and debugging
 * - Manual intervention for failed compensations
 * 
 * For this implementation, we use in-memory storage with the understanding
 * that production should use persistent storage.
 */
@Service
@Slf4j
public class SagaOrchestrator {
    
    // In-memory saga storage (use database in production)
    private final Map<UUID, SagaInstance> activeSagas = new ConcurrentHashMap<>();
    
    /**
     * Create and start a new saga
     * 
     * @param sagaType Type identifier for the saga
     * @param steps List of steps to execute
     * @return The created saga instance
     */
    public SagaInstance createSaga(String sagaType, List<SagaStep> steps) {
        SagaInstance saga = new SagaInstance(sagaType, steps);
        activeSagas.put(saga.getSagaId(), saga);
        
        log.info("Created saga {} of type '{}' with {} steps", 
            saga.getSagaId(), sagaType, steps.size());
        
        return saga;
    }
    
    /**
     * Execute saga synchronously
     * 
     * @param saga The saga to execute
     * @return true if successful, false if failed
     */
    public boolean executeSaga(SagaInstance saga) {
        log.info("Starting saga execution: {}", saga.getSagaId());
        
        while (!saga.isComplete()) {
            boolean canContinue = saga.executeNextStep();
            if (!canContinue && !saga.isComplete()) {
                // Step failed, compensation started
                break;
            }
        }
        
        boolean success = saga.isSuccessful();
        if (success) {
            log.info("Saga {} completed successfully", saga.getSagaId());
        } else {
            log.error("Saga {} failed: {}", saga.getSagaId(), saga.getFailureReason());
        }
        
        // In production: persist final state to database
        // cleanupSaga(saga.getSagaId());
        
        return success;
    }
    
    /**
     * Execute saga asynchronously
     * 
     * @param saga The saga to execute
     * @return CompletableFuture with success status
     */
    @Async("sagaExecutor")
    public CompletableFuture<Boolean> executeSagaAsync(SagaInstance saga) {
        return CompletableFuture.supplyAsync(() -> executeSaga(saga));
    }
    
    /**
     * Get saga by ID
     */
    public SagaInstance getSaga(UUID sagaId) {
        return activeSagas.get(sagaId);
    }
    
    /**
     * Get all active sagas
     */
    public Map<UUID, SagaInstance> getActiveSagas() {
        return new ConcurrentHashMap<>(activeSagas);
    }
    
    /**
     * Cancel a running saga (trigger compensation)
     */
    public void cancelSaga(UUID sagaId) {
        SagaInstance saga = activeSagas.get(sagaId);
        if (saga != null && !saga.isComplete()) {
            log.info("Cancelling saga {}", sagaId);
            saga.startCompensation();
        }
    }
    
    /**
     * Cleanup completed saga from memory
     */
    public void cleanupSaga(UUID sagaId) {
        SagaInstance removed = activeSagas.remove(sagaId);
        if (removed != null) {
            log.debug("Cleaned up saga {}", sagaId);
        }
    }
    
    /**
     * Execute single step (for retry scenarios)
     */
    public boolean retryStep(UUID sagaId, int stepIndex) {
        SagaInstance saga = activeSagas.get(sagaId);
        if (saga == null) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }
        
        if (stepIndex < 0 || stepIndex >= saga.getSteps().size()) {
            throw new IllegalArgumentException("Invalid step index: " + stepIndex);
        }
        
        SagaStep step = saga.getSteps().get(stepIndex);
        if (step.getStatus() != SagaStep.StepStatus.FAILED) {
            throw new IllegalStateException("Step is not in FAILED state: " + step.getStatus());
        }
        
        log.info("Retrying step {} of saga {}", step.getName(), sagaId);
        
        try {
            step.setStatus(SagaStep.StepStatus.PENDING);
            step.setRetryCount(0);
            step.execute();
            return true;
        } catch (Exception e) {
            log.error("Retry failed for step {}: {}", step.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Create Order Processing Saga
     * 
     * Example saga for the order processing workflow:
     * 1. Validate Order
     * 2. Reserve Inventory
     * 3. Process Payment
     * 4. Create Order
     * 5. Send Notification
     */
    public SagaInstance createOrderProcessingSaga(
            Runnable validateOrder,
            Runnable reserveInventory,
            Runnable releaseInventory,
            Runnable processPayment,
            Runnable refundPayment,
            Runnable createOrder,
            Runnable cancelOrder,
            Runnable sendNotification) {
        
        List<SagaStep> steps = List.of(
            SagaStep.builder()
                .name("ValidateOrder")
                .description("Validate order request")
                .action(validateOrder)
                .maxRetries(1)
                .build(),
                
            SagaStep.builder()
                .name("ReserveInventory")
                .description("Reserve items in inventory service")
                .action(reserveInventory)
                .compensation(releaseInventory)
                .maxRetries(3)
                .build(),
                
            SagaStep.builder()
                .name("ProcessPayment")
                .description("Process payment via payment service")
                .action(processPayment)
                .compensation(refundPayment)
                .maxRetries(3)
                .build(),
                
            SagaStep.builder()
                .name("CreateOrder")
                .description("Persist order to database")
                .action(createOrder)
                .compensation(cancelOrder)
                .maxRetries(3)
                .build(),
                
            SagaStep.builder()
                .name("SendNotification")
                .description("Send order confirmation notification")
                .action(sendNotification)
                .maxRetries(2) // No compensation - notifications can fail silently
                .build()
        );
        
        return createSaga("OrderProcessing", steps);
    }
}
