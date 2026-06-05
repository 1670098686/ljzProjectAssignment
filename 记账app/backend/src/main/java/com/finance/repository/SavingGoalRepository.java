package com.finance.repository;

import com.finance.entity.SavingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingGoalRepository extends JpaRepository<SavingGoal, Long> {

    Optional<SavingGoal> findByIdAndUserId(Long id, Long userId);

    Optional<SavingGoal> findByUserIdAndNameIgnoreCase(Long userId, String name);

    List<SavingGoal> findByUserIdOrderByDeadlineAsc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
