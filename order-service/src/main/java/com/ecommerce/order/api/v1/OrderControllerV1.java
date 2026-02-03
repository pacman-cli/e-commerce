package com.ecommerce.order.api.v1;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Order Controller API v1
 * 
 * Original API version with basic functionality.
 * Kept for backward compatibility with existing clients.
 * 
 * Supports versioning via:
 * - URL: /api/v1/orders
 * - Header: X-API-Version: v1
 * - Accept: application/vnd.ecommerce.v1+json
 * 
 * @deprecated Use v2 for new integrations. v1 will be sunset on 2025-06-01.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderControllerV1 {

    private final OrderService orderService;

    /**
     * Create Order - v1
     * Synchronous response (waits for completion)
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody CreateOrderRequest request) throws ExecutionException, InterruptedException {
        
        CompletableFuture<OrderResponse> future = orderService.createOrder(
                UUID.fromString(userId),
                userEmail,
                request
        );
        
        // v1 waits for completion (synchronous)
        OrderResponse response = future.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get Order by ID - v1
     * Basic response without additional metadata
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id) throws ExecutionException, InterruptedException {
        
        CompletableFuture<OrderResponse> future = orderService.getOrderById(id);
        OrderResponse response = future.get();
        return ResponseEntity.ok(response);
    }

    /**
     * Get User Orders - v1
     * Returns basic list without pagination metadata
     */
    @GetMapping
    public ResponseEntity<?> getUserOrders(
            @RequestHeader("X-User-Id") String userId) {
        
        // v1 returns list without pagination info
        var orders = orderService.getUserOrders(UUID.fromString(userId), 
            org.springframework.data.domain.PageRequest.of(0, 100));
        
        return ResponseEntity.ok(orders.getContent());
    }
}
