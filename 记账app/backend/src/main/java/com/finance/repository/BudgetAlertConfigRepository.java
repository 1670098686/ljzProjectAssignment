package com.finance.repository;

import com.finance.entity.BudgetAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetAlertConfigRepository extends JpaRepository<BudgetAlertConfig, Long> {

    /**
     * 根据用户ID查找预警配置
     * 
     * @param userId 用户ID
     * @return 预警配置
     */
    Optional<BudgetAlertConfig> findByUserId(Long userId);

    /**
     * 检查用户是否已存在预警配置
     * 
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByUserId(Long userId);
}