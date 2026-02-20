package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.model.Category;

public class CategoryMapper {

    private CategoryMapper() {}

    public static CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
