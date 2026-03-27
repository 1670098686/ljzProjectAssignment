package com.finance.repository;

import com.finance.entity.SavingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 储蓄记录仓库接口
 */
@Repository
public interface SavingRecordRepository extends JpaRepository<SavingRecord, Long> {

    /**
     * 根据储蓄目标ID查找记录
     */
    List<SavingRecord> findByGoalId(Long goalId);

    /**
     * 根据储蓄目标ID和记录类型查找记录
     */
    List<SavingRecord> findByGoalIdAndType(Long goalId, String type);

    /**
     * 根据用户ID查找记录
     */
    List<SavingRecord> findByUserId(Long userId);

    /**
     * 根据用户ID分页查询记录
     */
    Page<SavingRecord> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据条件查询记录
     */
    @Query("SELECT sr FROM SavingRecord sr WHERE " +
           "(:goalId IS NULL OR sr.goalId = :goalId) AND " +
           "(:type IS NULL OR sr.type = :type) AND " +
           "(:startDate IS NULL OR sr.recordDate >= :startDate) AND " +
           "(:endDate IS NULL OR sr.recordDate <= :endDate) AND " +
           "(:category IS NULL OR sr.category = :category) AND " +
           "sr.userId = :userId")
    List<SavingRecord> findByConditions(
        @Param("userId") Long userId,
        @Param("goalId") Long goalId,
        @Param("type") String type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("category") String category
    );

    /**
     * 计算储蓄目标的总存款金额
     */
    @Query("SELECT COALESCE(SUM(sr.amount), 0) FROM SavingRecord sr WHERE sr.goalId = :goalId AND sr.type = 'DEPOSIT'")
    BigDecimal sumDepositAmountByGoalId(@Param("goalId") Long goalId);

    /**
     * 计算储蓄目标的总取款金额
     */
    @Query("SELECT COALESCE(SUM(sr.amount), 0) FROM SavingRecord sr WHERE sr.goalId = :goalId AND sr.type = 'WITHDRAW'")
    BigDecimal sumWithdrawAmountByGoalId(@Param("goalId") Long goalId);

    /**
     * 计算用户的总存款金额
     */
    @Query("SELECT COALESCE(SUM(sr.amount), 0) FROM SavingRecord sr WHERE sr.userId = :userId AND sr.type = 'DEPOSIT'")
    BigDecimal sumDepositAmountByUserId(@Param("userId") Long userId);

    /**
     * 计算用户的总取款金额
     */
    @Query("SELECT COALESCE(SUM(sr.amount), 0) FROM SavingRecord sr WHERE sr.userId = :userId AND sr.type = 'WITHDRAW'")
    BigDecimal sumWithdrawAmountByUserId(@Param("userId") Long userId);

    /**
     * 统计用户储蓄记录数量
     */
    @Query("SELECT COUNT(sr) FROM SavingRecord sr WHERE sr.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 统计用户存款记录数量
     */
    @Query("SELECT COUNT(sr) FROM SavingRecord sr WHERE sr.userId = :userId AND sr.type = 'DEPOSIT'")
    long countDepositByUserId(@Param("userId") Long userId);

    /**
     * 统计用户取款记录数量
     */
    @Query("SELECT COUNT(sr) FROM SavingRecord sr WHERE sr.userId = :userId AND sr.type = 'WITHDRAW'")
    long countWithdrawByUserId(@Param("userId") Long userId);

    /**
     * 根据ID和用户ID查找记录
     */
    Optional<SavingRecord> findByIdAndUserId(Long id, Long userId);

    /**
     * 检查记录是否存在
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * 删除指定储蓄目标的所有记录
     */
    void deleteByGoalId(Long goalId);

    /**
     * 删除指定用户的所有记录
     */
    void deleteByUserId(Long userId);

    /**
     * 获取最近N条记录
     */
    List<SavingRecord> findTopNByUserIdOrderByRecordDateDesc(Long userId, Pageable pageable);
}