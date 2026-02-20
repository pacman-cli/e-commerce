package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Category with name '" + request.name() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
        .stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}
