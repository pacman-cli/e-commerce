package com.ecommerce.inventory.service.impl;

import com.ecommerce.inventory.dto.StockRequest;
import com.ecommerce.inventory.dto.StockResponse;
import com.ecommerce.inventory.exception.ResourceNotFoundException;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedissonClient redissonClient;

    // Redis lock key prefix
    private static final String LOCK_PREFIX = "inventory:lock:";

    @Override
    @Transactional
    public StockResponse initializeStock(UUID productId, Integer initialQuantity) {
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalArgumentException("Inventory profile already exists for product: " + productId);
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .build();

        return InventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponse getStockStatus(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId.toString()));
        return InventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public StockResponse reserveStock(StockRequest request) {
        UUID productId = request.productId();
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Wait up to 5 seconds to acquire lock, hold for a maximum of 10 seconds
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    Inventory inventory = inventoryRepository.findByProductId(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId.toString()));

                    if (inventory.getAvailableQuantity() < request.quantity()) {
                        throw new IllegalStateException("Insufficient stock for product: " + productId);
                    }

                    // Move from available to reserved
                    inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.quantity());
                    inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());

                    log.info("Reserved {} items for product {}", request.quantity(), productId);
                    return InventoryMapper.toResponse(inventoryRepository.save(inventory));
                } finally {
                    if (lock.isHeldByCurrentThread()) { // condition: check if the current thread is the one holding the lock
                        lock.unlock(); // release the lock
                    }
                }
            } else {
                throw new IllegalStateException("Could not acquire lock to reserve stock. System is busy.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupted status of the current thread
            throw new IllegalStateException("Stock reservation interrupted");
        }
    }

    @Override
    @Transactional
    public StockResponse releaseStock(StockRequest request) {
        UUID productId = request.productId();
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    Inventory inventory = inventoryRepository.findByProductId(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId.toString()));

                    // In a real saga, we would verify we actually hold this reservation
                    // For now, simply move back from reserved to available
                    if (inventory.getReservedQuantity() < request.quantity()) {
                        log.warn("Attempting to release more stock than reserved for product {}", productId);
                        // We could throw an error or just release what we have
                    }

                    inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.quantity());
                    inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - request.quantity()));

                    log.info("Released {} reserved items back to available for product {}", request.quantity(), productId);
                    return InventoryMapper.toResponse(inventoryRepository.save(inventory));
                } finally {
                    if (lock.isHeldByCurrentThread()) { // condition: check if the current thread is the one holding the lock
                        lock.unlock(); // release the lock
                    }
                }
            } else {
                throw new IllegalStateException("Could not acquire lock to release stock. System is busy.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupted status of the current thread
            throw new IllegalStateException("Stock release interrupted");
        }
    }

    @Override
    @Transactional
    public StockResponse confirmStockDeduction(StockRequest request) {
        UUID productId = request.productId();
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    Inventory inventory = inventoryRepository.findByProductId(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId.toString()));

                    // Permanently deduct from reserved (purchase confirmed)
                    if (inventory.getReservedQuantity() < request.quantity()) {
                        log.warn("Attempting to deduct more stock than reserved for product {}", productId);
                    }

                    inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - request.quantity()));

                    log.info("Permanently deducted {} items from reserved for product {}", request.quantity(), productId);
                    return InventoryMapper.toResponse(inventoryRepository.save(inventory));
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new IllegalStateException("Could not acquire lock to deduce stock. System is busy.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Stock deduction interrupted");
        }
    }
}
