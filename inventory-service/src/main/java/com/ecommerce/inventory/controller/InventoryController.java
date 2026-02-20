package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.StockRequest;
import com.ecommerce.inventory.dto.StockResponse;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/initialize/{productId}")
    public ResponseEntity<StockResponse> initializeStock(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {
        log.info("Initializing inventory for product: {} with quantity: {}", productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.initializeStock(productId, quantity));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> getStockStatus(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStockStatus(productId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<StockResponse> reserveStock(@Valid @RequestBody StockRequest request) {
        log.info("Requesting lock to reserve {} items of product: {}", request.quantity(), request.productId());
        return ResponseEntity.ok(inventoryService.reserveStock(request));
    }

    @PostMapping("/release")
    public ResponseEntity<StockResponse> releaseStock(@Valid @RequestBody StockRequest request) {
        log.info("Requesting lock to release {} items of product: {}", request.quantity(), request.productId());
        return ResponseEntity.ok(inventoryService.releaseStock(request));
    }

    @PostMapping("/deduct")
    public ResponseEntity<StockResponse> confirmStockDeduction(@Valid @RequestBody StockRequest request) {
        log.info("Requesting lock to deduct {} items of product: {}", request.quantity(), request.productId());
        return ResponseEntity.ok(inventoryService.confirmStockDeduction(request));
    }
}
