package com.finance.repository;

import com.finance.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问接口
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    /**
     * 根据用户ID分页查询操作日志
     */
    Page<OperationLog> findByUserIdOrderByOperationTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据操作类型分页查询操作日志
     */
    Page<OperationLog> findByOperationTypeOrderByOperationTimeDesc(String operationType, Pageable pageable);

    /**
     * 根据用户ID和操作类型分页查询操作日志
     */
    Page<OperationLog> findByUserIdAndOperationTypeOrderByOperationTimeDesc(
            Long userId, String operationType, Pageable pageable);

    /**
     * 根据时间范围分页查询操作日志
     */
    Page<OperationLog> findByOperationTimeBetweenOrderByOperationTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据用户ID和时间范围分页查询操作日志
     */
    Page<OperationLog> findByUserIdAndOperationTimeBetweenOrderByOperationTimeDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据状态分页查询操作日志
     */
    Page<OperationLog> findByStatusOrderByOperationTimeDesc(String status, Pageable pageable);

    /**
     * 根据业务对象类型和ID查询操作日志
     */
    List<OperationLog> findByBusinessTypeAndBusinessIdOrderByOperationTimeDesc(
            String businessType, Long businessId);

    /**
     * 统计用户在指定时间范围内的操作次数
     */
    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.userId = :userId " +
           "AND o.operationTime BETWEEN :startTime AND :endTime")
    long countByUserIdAndOperationTimeBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各类操作的次数
     */
    @Query("SELECT o.operationType, COUNT(o) FROM OperationLog o " +
           "WHERE o.operationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY o.operationType ORDER BY COUNT(o) DESC")
    List<Object[]> countByOperationTypeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户各类操作的次数
     */
    @Query("SELECT o.operationType, COUNT(o) FROM OperationLog o " +
           "WHERE o.userId = :userId AND o.operationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY o.operationType ORDER BY COUNT(o) DESC")
    List<Object[]> countByUserIdAndOperationTypeBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的错误操作
     */
    @Query("SELECT o FROM OperationLog o WHERE o.status = 'FAILURE' " +
           "ORDER BY o.operationTime DESC")
    List<OperationLog> findRecentFailures(Pageable pageable);

    /**
     * 清理过期日志（超过指定天数的日志）
     */
    @Query("DELETE FROM OperationLog o WHERE o.operationTime < :cutoffDate")
    void deleteByOperationTimeBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 按天统计操作量
     */
    @Query("SELECT DATE(o.operationTime), COUNT(o) FROM OperationLog o " +
           "WHERE o.operationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(o.operationTime) ORDER BY DATE(o.operationTime)")
    List<Object[]> countByDateBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}