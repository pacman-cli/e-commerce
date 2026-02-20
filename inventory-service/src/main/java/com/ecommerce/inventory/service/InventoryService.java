package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.StockRequest;
import com.ecommerce.inventory.dto.StockResponse;

import java.util.UUID;

public interface InventoryService {
    StockResponse initializeStock(UUID productId, Integer initialQuantity);
    StockResponse getStockStatus(UUID productId);
    StockResponse reserveStock(StockRequest request);
    StockResponse releaseStock(StockRequest request);
    StockResponse confirmStockDeduction(StockRequest request);
}
