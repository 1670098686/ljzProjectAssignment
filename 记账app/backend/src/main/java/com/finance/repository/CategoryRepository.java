package com.finance.repository;

import com.finance.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdAndType(Long userId, Integer type);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Category> findByUserIdAndDefaultCategoryFalse(Long userId);

    java.util.Optional<Category> findByIdAndUserId(Long id, Long userId);

    java.util.Optional<Category> findByUserIdAndNameIgnoreCaseAndType(Long userId, String name, Integer type);

    /**
     * 按用户ID和类型查找分类，并按排序值升序排列
     */
    List<Category> findByUserIdAndTypeOrderBySortOrderAsc(Long userId, Integer type);
    
    /**
     * 按用户ID查找分类，并按排序值升序排列
     */
    List<Category> findByUserIdOrderBySortOrderAsc(Long userId);
}
