package com.finance.service.impl;

import com.finance.cache.CacheNames;
import com.finance.cache.CacheSupport;
import com.finance.context.UserContext;
import com.finance.dto.BudgetDto;
import com.finance.dto.CreateBudgetRequest;
import com.finance.dto.UpdateBudgetRequest;
import com.finance.entity.Budget;
import com.finance.entity.Category;
import com.finance.exception.BusinessException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.BudgetRepository;
import com.finance.repository.CategoryRepository;
import com.finance.service.BudgetService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserContext userContext;
    private final CacheSupport cacheSupport;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
                             CategoryRepository categoryRepository,
                             UserContext userContext,
                             CacheSupport cacheSupport) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userContext = userContext;
        this.cacheSupport = cacheSupport;
    }

    @Override
    public List<BudgetDto> listBudgets(Integer year, Integer month) {
        Long userId = userContext.getCurrentUserId();
        String cacheKey = buildBudgetCacheKey(userId, year, month);
        return cacheSupport.getOrLoad(cacheKey, () -> loadBudgets(userId, year, month), 86400, CacheNames.BUDGET_MONTHLY);
    }

    @Override
    public BudgetDto getBudget(Long id) {
        Objects.requireNonNull(id, "Budget id must not be null");
        Long userId = userContext.getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
        return toDto(budget);
    }

    @Override
    public BudgetDto createBudget(CreateBudgetRequest request) {
        Objects.requireNonNull(request, "CreateBudgetRequest must not be null");
        validateYearMonth(request.getYear(), request.getMonth());

        Long userId = userContext.getCurrentUserId();
        Category category = getCategory(request.getCategoryId(), userId);

        ensureBudgetUnique(category.getId(), request.getYear(), request.getMonth(), null, userId);

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());

        Budget saved = budgetRepository.save(budget);
        clearBudgetCache();
        return toDto(saved);
    }

    @Override
    public BudgetDto updateBudget(Long id, UpdateBudgetRequest request) {
        Objects.requireNonNull(id, "Budget id must not be null");
        Objects.requireNonNull(request, "UpdateBudgetRequest must not be null");
        validateYearMonth(request.getYear(), request.getMonth());

        Long userId = userContext.getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));

        Category category = getCategory(request.getCategoryId(), userId);
        ensureBudgetUnique(category.getId(), request.getYear(), request.getMonth(), id, userId);

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());

        Budget saved = budgetRepository.save(budget);
        clearBudgetCache();
        return toDto(saved);
    }

    @Override
    public void deleteBudget(Long id) {
        Objects.requireNonNull(id, "Budget id must not be null");
        Long userId = userContext.getCurrentUserId();
        if (!budgetRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Budget not found: " + id);
        }
        budgetRepository.deleteById(id);
        clearBudgetCache();
    }

    private List<BudgetDto> loadBudgets(Long userId, Integer year, Integer month) {
        if (year == null && month == null) {
            return budgetRepository.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
        }
        validateYearMonth(year, month);
        return budgetRepository.findByUserIdAndYearAndMonth(userId, year, month).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validateYearMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new BusinessException("Year and month are required");
        }
        if (year < 2000 || year > 2100) {
            throw new BusinessException("Year must be between 2000 and 2100");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
    }

    private BudgetDto toDto(Budget budget) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setCategoryId(budget.getCategory().getId());
        dto.setCategoryName(budget.getCategory().getName());
        dto.setAmount(budget.getAmount());
        dto.setYear(budget.getYear());
        dto.setMonth(budget.getMonth());
        return dto;
    }

    private void ensureBudgetUnique(Long categoryId, Integer year, Integer month, Long excludeId, Long userId) {
        budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(userId, categoryId, year, month)
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new BusinessException("Budget already exists for this category and month");
                });
    }

    private Category getCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            throw new BusinessException("Category id is required");
        }
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private String buildBudgetCacheKey(Long userId, Integer year, Integer month) {
        return String.join(":",
                String.valueOf(userId),
                year == null ? "all" : year.toString(),
                month == null ? "all" : month.toString());
    }

    private void clearBudgetCache() {
        // 使用cacheSupport批量删除预算缓存
        cacheSupport.deleteBatch(CacheNames.BUDGET_MONTHLY + ":*" + userContext.getCurrentUserId() + ":*:");
    }
}
