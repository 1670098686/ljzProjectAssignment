package com.finance.repository;

import com.finance.entity.BudgetAlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BudgetAlertHistoryRepository extends JpaRepository<BudgetAlertHistory, Long> {

    /**
     * 根据用户ID和筛选条件查询预警历史记录
     * 
     * @param userId 用户ID
     * @param year 年份（可选）
     * @param month 月份（可选）
     * @param alertLevels 预警级别列表（可选）
     * @param categoryId 分类ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageable 分页参数
     * @return 预警历史记录分页结果
     */
    @Query("""
        SELECT h FROM BudgetAlertHistory h 
        WHERE h.userId = :userId 
        AND (:year IS NULL OR h.year = :year)
        AND (:month IS NULL OR h.month = :month)
        AND (:alertLevels IS NULL OR h.alertLevel IN :alertLevels)
        AND (:categoryId IS NULL OR h.categoryId = :categoryId)
        AND (:startDate IS NULL OR h.alertTime >= :startDate)
        AND (:endDate IS NULL OR h.alertTime <= :endDate)
        ORDER BY h.alertTime DESC
    """)
    Page<BudgetAlertHistory> findByUserIdAndFilters(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("alertLevels") List<String> alertLevels,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 统计用户指定月份的预警次数
     * 
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 预警次数
     */
    @Query("""
        SELECT COUNT(h) FROM BudgetAlertHistory h 
        WHERE h.userId = :userId 
        AND h.year = :year 
        AND h.month = :month
    """)
    long countByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 统计用户各分类的预警次数
     * 
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 预警统计列表
     */
    @Query("""
        SELECT h.categoryId, h.categoryName, COUNT(h) as alertCount
        FROM BudgetAlertHistory h 
        WHERE h.userId = :userId 
        AND h.year = :year 
        AND h.month = :month
        GROUP BY h.categoryId, h.categoryName
        ORDER BY alertCount DESC
    """)
    List<Object[]> countAlertsByCategory(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}