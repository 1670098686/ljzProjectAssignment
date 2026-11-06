package com.finance.repository;

import com.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    List<Budget> findByUserId(Long userId);

    List<Budget> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, Integer year, Integer month);

    boolean existsByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, Integer year, Integer month);

    boolean existsByIdAndUserId(Long id, Long userId);
}
