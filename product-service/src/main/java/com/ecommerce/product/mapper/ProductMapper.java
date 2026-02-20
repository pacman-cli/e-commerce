package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;

public class ProductMapper {

    private ProductMapper() {}

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                CategoryMapper.toResponse(product.getCategory()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
