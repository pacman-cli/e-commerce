package com.ecommerce.inventory.dto;

import java.util.UUID;

public record StockResponse(
        UUID productId,
        Integer availableQuantity,
        Integer reservedQuantity,
        boolean inStock
) {
}
