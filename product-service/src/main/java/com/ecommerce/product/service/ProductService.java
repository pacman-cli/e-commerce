package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(UUID id);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable);
    Page<ProductResponse> searchProducts(String name, Pageable pageable);
    void reduceStock(UUID id, int quantity);
}
