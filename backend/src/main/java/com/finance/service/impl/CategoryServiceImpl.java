package com.finance.service.impl;

import com.finance.context.UserContext;
import com.finance.dto.CategoryDto;
import com.finance.dto.CreateCategoryRequest;
import com.finance.dto.IconPresetDto;
import com.finance.dto.UpdateCategoryRequest;
import com.finance.entity.Category;
import com.finance.exception.BusinessException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.CategoryRepository;
import com.finance.service.CategoryService;
import com.finance.service.IconService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final int TYPE_INCOME = 1;
    private static final int TYPE_EXPENSE = 2;

    private final CategoryRepository categoryRepository;
    private final UserContext userContext;
    private final IconService iconService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, 
                              UserContext userContext,
                              IconService iconService) {
        this.categoryRepository = categoryRepository;
        this.userContext = userContext;
        this.iconService = iconService;
    }

    @Override
    public List<CategoryDto> listCategories(Integer type) {
        Long userId = userContext.getCurrentUserId();
        if (type != null) {
            validateType(type);
            return categoryRepository.findByUserIdAndTypeOrderBySortOrderAsc(userId, type).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return categoryRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long id) {
        Objects.requireNonNull(id, "Category id must not be null");
        Long userId = userContext.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return toDto(category);
    }

    @Override
    public CategoryDto createCategory(CreateCategoryRequest request) {
        Objects.requireNonNull(request, "CreateCategoryRequest must not be null");
        validateType(request.getType());

        Long userId = userContext.getCurrentUserId();
        ensureCategoryUnique(userId, request.getName(), request.getType(), null);

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName().trim());
        category.setIcon(resolveIcon(request.getIcon()));
        category.setType(request.getType());

        return toDto(categoryRepository.save(category));
    }

    @Override
    public CategoryDto updateCategory(Long id, UpdateCategoryRequest request) {
        Objects.requireNonNull(id, "Category id must not be null");
        Objects.requireNonNull(request, "UpdateCategoryRequest must not be null");
        validateType(request.getType());

        Long userId = userContext.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        ensureCategoryUnique(userId, request.getName(), request.getType(), id);
        category.setName(request.getName().trim());
        category.setIcon(resolveIcon(request.getIcon()));
        category.setType(request.getType());

        return toDto(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {
        Objects.requireNonNull(id, "Category id must not be null");
        Long userId = userContext.getCurrentUserId();
        if (!categoryRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private void validateType(Integer type) {
        if (type == null || (type != TYPE_INCOME && type != TYPE_EXPENSE)) {
            throw new BusinessException("Category type must be 1 (income) or 2 (expense)");
        }
    }

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setType(category.getType());
        dto.setDefaultCategory(category.isDefaultCategory());
        dto.setSortOrder(category.getSortOrder());
        return dto;
    }

    private void ensureCategoryUnique(Long userId, String name, Integer type, Long currentId) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BusinessException("Category name must not be blank");
        }
        categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, trimmed, type)
                .ifPresent(existing -> {
                    if (currentId == null || !existing.getId().equals(currentId)) {
                        throw new BusinessException("Category already exists with the same name and type");
                    }
                });
    }

    private String resolveIcon(String icon) {
        if (icon == null) {
            return "default_icon.png";
        }
        String trimmed = icon.trim();
        if (trimmed.isEmpty()) {
            return "default_icon.png";
        }
        
        // 验证图标是否有效
        if (!iconService.isValidIcon(trimmed)) {
            throw new BusinessException("Invalid icon name: " + trimmed);
        }
        
        return trimmed;
    }
    
    @Override
    public List<IconPresetDto> getAvailableIcons() {
        return iconService.getIconPresets();
    }
    
    @Override
    public List<IconPresetDto> getIconsByCategory(String category) {
        return iconService.getIconPresetsByCategory(category);
    }
    
    @Override
    @Transactional
    public void reorderCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("分类ID列表不能为空");
        }
        
        // 获取当前用户的所有分类
        Long currentUserId = userContext.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserId(currentUserId);
        
        // 验证所有分类ID都属于当前用户
        Set<Long> userCategoryIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        
        for (Long categoryId : categoryIds) {
            if (!userCategoryIds.contains(categoryId)) {
                throw new IllegalArgumentException("分类ID " + categoryId + " 不属于当前用户");
            }
        }
        
        // 更新排序值
        for (int i = 0; i < categoryIds.size(); i++) {
            Long categoryId = categoryIds.get(i);
            Optional<Category> categoryOpt = categories.stream()
                    .filter(c -> c.getId().equals(categoryId))
                    .findFirst();
            
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                category.setSortOrder(i);
                categoryRepository.save(category);
            }
        }
    }
    
    @Override
    @Transactional
    public CategoryDto updateCategorySortOrder(Long id, Integer sortOrder) {
        if (id == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("排序值必须为非负整数");
        }
        
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getUserId().equals(userContext.getCurrentUserId()))
                .orElseThrow(() -> new IllegalArgumentException("分类不存在或无权限"));
        
        category.setSortOrder(sortOrder);
        Category savedCategory = categoryRepository.save(category);
        
        return toDto(savedCategory);
    }
}
