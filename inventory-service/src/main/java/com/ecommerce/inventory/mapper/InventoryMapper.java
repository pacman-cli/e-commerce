package com.ecommerce.inventory.mapper;

import com.ecommerce.inventory.dto.StockResponse;
import com.ecommerce.inventory.model.Inventory;

public class InventoryMapper {

    private InventoryMapper() {}

    public static StockResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        return new StockResponse(
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity() > 0
        );
    }
}
