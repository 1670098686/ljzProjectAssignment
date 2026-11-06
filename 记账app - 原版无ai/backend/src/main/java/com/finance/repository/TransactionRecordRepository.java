package com.finance.repository;

import com.finance.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    @Query("SELECT t FROM TransactionRecord t WHERE t.userId = :userId " +
            "AND (:start IS NULL OR t.transactionDate >= :start) " +
            "AND (:end IS NULL OR t.transactionDate <= :end) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId)")
    List<TransactionRecord> findByFilter(@Param("userId") Long userId,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end,
                                         @Param("type") Integer type,
                                         @Param("categoryId") Long categoryId);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 1 THEN t.amount ELSE 0 END),0) FROM TransactionRecord t " +
            "WHERE t.userId = :userId AND EXTRACT(YEAR FROM t.transactionDate) = :year AND EXTRACT(MONTH FROM t.transactionDate) = :month")
    java.math.BigDecimal getMonthlyIncome(@Param("userId") Long userId,
                                          @Param("year") int year,
                                          @Param("month") int month);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 2 THEN t.amount ELSE 0 END),0) FROM TransactionRecord t " +
            "WHERE t.userId = :userId AND EXTRACT(YEAR FROM t.transactionDate) = :year AND EXTRACT(MONTH FROM t.transactionDate) = :month")
    java.math.BigDecimal getMonthlyExpense(@Param("userId") Long userId,
                                           @Param("year") int year,
                                           @Param("month") int month);

    @Query("SELECT t.category.name, COALESCE(SUM(t.amount),0) FROM TransactionRecord t " +
            "WHERE t.userId = :userId AND t.transactionDate BETWEEN :start AND :end " +
            "AND (:type IS NULL OR t.type = :type) " +
            "GROUP BY t.category.name")
    List<Object[]> aggregateByCategory(@Param("userId") Long userId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       @Param("type") Integer type);

    @Query("SELECT t.transactionDate, " +
            "COALESCE(SUM(CASE WHEN t.type = 1 THEN t.amount ELSE 0 END),0), " +
            "COALESCE(SUM(CASE WHEN t.type = 2 THEN t.amount ELSE 0 END),0) " +
            "FROM TransactionRecord t WHERE t.userId = :userId AND t.transactionDate BETWEEN :start AND :end " +
            "GROUP BY t.transactionDate ORDER BY t.transactionDate")
    List<Object[]> aggregateTrend(@Param("userId") Long userId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    @Query("SELECT t.category.id, COALESCE(SUM(t.amount),0) FROM TransactionRecord t " +
            "WHERE t.userId = :userId AND t.type = 2 " +
            "AND EXTRACT(YEAR FROM t.transactionDate) = :year AND EXTRACT(MONTH FROM t.transactionDate) = :month " +
            "GROUP BY t.category.id")
    List<Object[]> sumMonthlyExpenseByCategory(@Param("userId") Long userId,
                                               @Param("year") int year,
                                               @Param("month") int month);

    Optional<TransactionRecord> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
