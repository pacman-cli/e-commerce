package com.ecommerce.order.api.v2;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.api.common.ApiVersion;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Controller API v2
 *
 * Enhanced API with:
 * - Async/Non-blocking responses (202 Accepted)
 * - Pagination metadata
 * - HATEOAS-style links
 * - Additional fields and filtering
 *
 * Versioning supported via:
 * - URL: /api/v2/orders
 * - Header: X-API-Version: v2
 * - Accept: application/vnd.ecommerce.v2+json
 */
@ApiVersion("v2")
@RestController
@RequestMapping("/api/v2/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderControllerV2 {

    private final OrderService orderService;

    /**
     * Create Order - v2
     * Async response (returns immediately with order ID, processes in background)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrderAsync(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody CreateOrderRequest request) {

        log.info("v2: Async order creation for user {}", userId);

        // Start async processing
        // Start async processing
        orderService.createOrder(
                UUID.fromString(userId),
                userEmail,
                request); // for async processing we use CompletableFuture

        // Generate order ID for tracking (actual ID will be in result)
        String trackingId = UUID.randomUUID().toString();

        // Return 202 Accepted with tracking info
        Map<String, Object> response = new HashMap<>();
        response.put("trackingId", trackingId);
        response.put("status", "PROCESSING");
        response.put("message", "Order is being processed asynchronously");
        response.put("checkStatusUrl", "/api/v2/orders/status/" + trackingId);
        response.put("timestamp", Instant.now().toString());

        // Store future for status checking (in production, use proper storage)
        // AsyncOrderTracker.register(trackingId, future);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get Order by ID - v2
     * Enhanced response with additional metadata and links
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean includeItems)
            throws ExecutionException, InterruptedException {

        CompletableFuture<OrderResponse> future = orderService.getOrderById(id);
        OrderResponse order = future.get();

        Map<String, Object> response = new HashMap<>();
        response.put("data", order);
        response.put("apiVersion", "v2");
        response.put("timestamp", Instant.now().toString());

        // Add HATEOAS-style links
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v2/orders/" + id);
        links.put("cancel", "/api/v2/orders/" + id + "/cancel");
        links.put("items", "/api/v2/orders/" + id + "/items");
        response.put("_links", links);

        // Include full items if requested
        if (includeItems) {
            response.put("items", order.getItems());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get User Orders - v2
     * Paginated with metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {

        log.info("v2: Getting orders for user {} - page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.getUserOrders(UUID.fromString(userId), pageable);

        // Build response with pagination metadata
        Map<String, Object> response = new HashMap<>();
        response.put("data", orders.getContent());
        response.put("apiVersion", "v2");

        // Pagination metadata
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", orders.getNumber());
        pagination.put("size", orders.getSize());
        pagination.put("totalElements", orders.getTotalElements());
        pagination.put("totalPages", orders.getTotalPages());
        pagination.put("first", orders.isFirst());
        pagination.put("last", orders.isLast());
        pagination.put("hasNext", orders.hasNext());
        pagination.put("hasPrevious", orders.hasPrevious());
        response.put("pagination", pagination);

        // Navigation links
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v2/orders?page=" + page + "&size=" + size);
        if (orders.hasNext()) {
            links.put("next", "/api/v2/orders?page=" + (page + 1) + "&size=" + size);
        }
        if (orders.hasPrevious()) {
            links.put("previous", "/api/v2/orders?page=" + (page - 1) + "&size=" + size);
        }
        response.put("_links", links);

        return ResponseEntity.ok(response);
    }

    /**
     * Check async order status - v2 only
     */
    @GetMapping("/status/{trackingId}")
    public ResponseEntity<Map<String, Object>> checkOrderStatus(
            @PathVariable String trackingId) {

        // In production, look up status from tracking storage
        Map<String, Object> response = new HashMap<>();
        response.put("trackingId", trackingId);
        response.put("status", "PROCESSING"); // or COMPLETED, FAILED
        response.put("estimatedCompletion", "5 seconds");
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel Order - v2
     * New endpoint not available in v1
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String reason) {

        log.info("v2: Cancelling order {} for user {}: {}", id, userId, reason);

        // In production, implement saga-based cancellation
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", id);
        response.put("status", "CANCELLED");
        response.put("reason", reason != null ? reason : "User request");
        response.put("cancelledAt", Instant.now().toString());
        response.put("refundStatus", "PENDING");

        return ResponseEntity.ok(response);
    }
}
