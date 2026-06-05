package com.campus.trade.service;

import com.campus.trade.dto.category.CategoryRequest;
import com.campus.trade.dto.category.CategoryResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Category;
import com.campus.trade.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listEnabled() {
        // 多级分类：优先返回顶级分类，其次返回子分类
        return categoryRepository.findByEnabledTrue().stream()
            .sorted(Comparator
                .comparing((Category c) -> c.getParentId() != null)
                .thenComparing(c -> c.getParentId() == null ? 0L : c.getParentId())
                .thenComparing(c -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                .thenComparing(Category::getId))
            .map(CategoryService::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getEnabledEntity(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "分类不存在"));
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "分类已禁用");
        }
        return category;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String code = normalizeCode(request.getCode());
        if (categoryRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "分类编码已存在");
        }
        Category category = new Category();
        category.setCode(code);
        category.setName(request.getName().trim());
        category.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "父分类不存在"));
            category.setParentId(parent.getId());
        }
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "分类不存在"));
        String code = normalizeCode(request.getCode());
        if (!code.equalsIgnoreCase(category.getCode()) && categoryRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "分类编码已存在");
        }
        category.setCode(code);
        category.setName(request.getName().trim());
        category.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "父分类不能是自己");
        }
        if (request.getParentId() == null) {
            category.setParentId(null);
        } else {
            Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "父分类不存在"));
            category.setParentId(parent.getId());
        }
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            return;
        }
        categoryRepository.deleteById(id);
    }

    private static String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        return code.trim().toUpperCase(Locale.ENGLISH);
    }

    private static CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setCode(category.getCode());
        response.setName(category.getName());
        response.setEnabled(category.getEnabled());
        response.setSortOrder(category.getSortOrder());
        response.setParentId(category.getParentId());
        return response;
    }
}
