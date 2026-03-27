package com.finance.service.impl;

import com.finance.context.UserContext;
import com.finance.dto.CreateSavingGoalRequest;
import com.finance.dto.SavingGoalDto;
import com.finance.dto.UpdateSavingGoalAmountRequest;
import com.finance.dto.UpdateSavingGoalRequest;
import com.finance.entity.SavingGoal;
import com.finance.event.DomainEventPublisher;
import com.finance.event.SavingGoalAchievedEvent;
import com.finance.exception.BusinessException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.SavingGoalRepository;
import com.finance.service.SavingGoalService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SavingGoalServiceImpl implements SavingGoalService {

    private final SavingGoalRepository savingGoalRepository;
    private final UserContext userContext;
    private final DomainEventPublisher eventPublisher;

    public SavingGoalServiceImpl(SavingGoalRepository savingGoalRepository,
                                 UserContext userContext,
                                 DomainEventPublisher eventPublisher) {
        this.savingGoalRepository = savingGoalRepository;
        this.userContext = userContext;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<SavingGoalDto> listSavingGoals() {
        Long userId = userContext.getCurrentUserId();
        return savingGoalRepository.findByUserIdOrderByDeadlineAsc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SavingGoalDto getSavingGoal(Long id) {
        Objects.requireNonNull(id, "Saving goal id must not be null");
        Long userId = userContext.getCurrentUserId();
        SavingGoal goal = savingGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found: " + id));
        return toDto(goal);
    }

    @Override
    public SavingGoalDto createSavingGoal(CreateSavingGoalRequest request) {
        Objects.requireNonNull(request, "CreateSavingGoalRequest must not be null");
        validateProgress(request.getCurrentAmount(), request.getTargetAmount());
        Long userId = userContext.getCurrentUserId();
        ensureNameUnique(request.getName(), null, userId);

        SavingGoal goal = new SavingGoal();
        goal.setUserId(userId);
        goal.setName(request.getName().trim());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount());
        goal.setDeadline(request.getDeadline());
        goal.setDescription(request.getDescription());

        SavingGoal saved = savingGoalRepository.save(goal);
        publishGoalAchievedIfNeeded(saved, null);
        return toDto(saved);
    }

    @Override
    public SavingGoalDto updateSavingGoal(Long id, UpdateSavingGoalRequest request) {
        Objects.requireNonNull(id, "Saving goal id must not be null");
        Objects.requireNonNull(request, "UpdateSavingGoalRequest must not be null");
        validateProgress(request.getCurrentAmount(), request.getTargetAmount());

        Long userId = userContext.getCurrentUserId();
        SavingGoal goal = savingGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found: " + id));

        ensureNameUnique(request.getName(), id, userId);

        BigDecimal previousAmount = goal.getCurrentAmount();
        goal.setName(request.getName().trim());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount());
        goal.setDeadline(request.getDeadline());
        goal.setDescription(request.getDescription());

        SavingGoal saved = savingGoalRepository.save(goal);
        publishGoalAchievedIfNeeded(saved, previousAmount);
        return toDto(saved);
    }

    @Override
    public SavingGoalDto updateCurrentAmount(Long id, UpdateSavingGoalAmountRequest request) {
        Objects.requireNonNull(id, "Saving goal id must not be null");
        Objects.requireNonNull(request, "UpdateSavingGoalAmountRequest must not be null");

        Long userId = userContext.getCurrentUserId();
        SavingGoal goal = savingGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found: " + id));

        validateProgress(request.getCurrentAmount(), goal.getTargetAmount());
        BigDecimal previousAmount = goal.getCurrentAmount();
        goal.setCurrentAmount(request.getCurrentAmount());
        SavingGoal saved = savingGoalRepository.save(goal);
        publishGoalAchievedIfNeeded(saved, previousAmount);
        return toDto(saved);
    }

    @Override
    public void deleteSavingGoal(Long id) {
        Objects.requireNonNull(id, "Saving goal id must not be null");
        Long userId = userContext.getCurrentUserId();
        if (!savingGoalRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Saving goal not found: " + id);
        }
        savingGoalRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        Objects.requireNonNull(id, "Saving goal id must not be null");
        Long userId = userContext.getCurrentUserId();
        return savingGoalRepository.existsByIdAndUserId(id, userId);
    }

    private void ensureNameUnique(String name, Long currentId, Long userId) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BusinessException("Saving goal name must not be blank");
        }
        savingGoalRepository.findByUserIdAndNameIgnoreCase(userId, trimmed).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new BusinessException("Saving goal name already exists");
            }
        });
    }

    private void validateProgress(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (currentAmount == null || targetAmount == null) {
            throw new BusinessException("Target and current amount are required");
        }
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Target amount must be greater than zero");
        }
        if (currentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Current amount cannot be negative");
        }
        if (currentAmount.compareTo(targetAmount) > 0) {
            throw new BusinessException("Current amount cannot exceed target amount");
        }
    }

    private SavingGoalDto toDto(SavingGoal goal) {
        SavingGoalDto dto = new SavingGoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setDeadline(goal.getDeadline());
        dto.setDescription(goal.getDescription());
        dto.setProgressPercentage(calculateProgressPercentage(goal));
        return dto;
    }

    private BigDecimal calculateProgressPercentage(SavingGoal goal) {
        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal progress = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        return progress.multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
    }

    private void publishGoalAchievedIfNeeded(SavingGoal goal, BigDecimal previousAmount) {
        if (goal == null || goal.getTargetAmount() == null) {
            return;
        }
        BigDecimal currentAmount = goal.getCurrentAmount();
        if (currentAmount == null) {
            return;
        }
        boolean achievedNow = currentAmount.compareTo(goal.getTargetAmount()) >= 0;
        boolean achievedBefore = previousAmount != null && previousAmount.compareTo(goal.getTargetAmount()) >= 0;
        if (!achievedBefore && achievedNow) {
            eventPublisher.publish(new SavingGoalAchievedEvent(
                    goal.getUserId(),
                    goal.getId(),
                    goal.getTargetAmount(),
                    goal.getUpdateTime()));
        }
    }

    @Override
    public BigDecimal calculateProgressPercentage(Long goalId) {
        SavingGoal goal = getSavingGoalEntity(goalId);
        return calculateProgressPercentage(goal);
    }

    @Override
    public BigDecimal getRemainingAmount(Long goalId) {
        SavingGoal goal = getSavingGoalEntity(goalId);
        BigDecimal targetAmount = goal.getTargetAmount();
        BigDecimal currentAmount = goal.getCurrentAmount();
        
        if (targetAmount == null || currentAmount == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal remaining = targetAmount.subtract(currentAmount);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    @Override
    public long getRemainingDays(Long goalId) {
        SavingGoal goal = getSavingGoalEntity(goalId);
        if (goal.getDeadline() == null) {
            return Long.MAX_VALUE;
        }
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.now(), goal.getDeadline());
        return Math.max(0, days);
    }

    @Override
    public BigDecimal getDailySavingAmount(Long goalId) {
        BigDecimal remainingAmount = getRemainingAmount(goalId);
        long remainingDays = getRemainingDays(goalId);
        
        if (remainingDays <= 0 || remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return remainingAmount.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isGoalCompleted(Long goalId) {
        SavingGoal goal = getSavingGoalEntity(goalId);
        return goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0;
    }

    @Override
    public SavingGoalStats getSavingGoalStats() {
        Long userId = userContext.getCurrentUserId();
        List<SavingGoal> goals = savingGoalRepository.findByUserIdOrderByDeadlineAsc(userId);
        
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        int completedGoals = 0;
        int overdueGoals = 0;
        
        for (SavingGoal goal : goals) {
            totalTarget = totalTarget.add(goal.getTargetAmount());
            totalCurrent = totalCurrent.add(goal.getCurrentAmount());
            
            if (isGoalCompleted(goal.getId())) {
                completedGoals++;
            }
            
            if (goal.getDeadline() != null && 
                goal.getDeadline().isBefore(java.time.LocalDate.now()) &&
                !isGoalCompleted(goal.getId())) {
                overdueGoals++;
            }
        }
        
        BigDecimal totalProgress = totalTarget.compareTo(BigDecimal.ZERO) > 0 ?
            totalCurrent.multiply(BigDecimal.valueOf(100))
                .divide(totalTarget, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        BigDecimal totalRemaining = totalTarget.subtract(totalCurrent);
        
        return new SavingGoalStatsImpl(
            totalTarget, totalCurrent, totalProgress, totalRemaining,
            goals.size(), completedGoals, goals.size() - completedGoals, overdueGoals);
    }

    private SavingGoal getSavingGoalEntity(Long goalId) {
        Objects.requireNonNull(goalId, "Saving goal id must not be null");
        Long userId = userContext.getCurrentUserId();
        return savingGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found: " + goalId));
    }

    /**
     * 储蓄目标统计信息实现类
     */
    private static class SavingGoalStatsImpl implements SavingGoalStats {
        private final BigDecimal totalTargetAmount;
        private final BigDecimal totalCurrentAmount;
        private final BigDecimal totalProgressPercentage;
        private final BigDecimal totalRemainingAmount;
        private final int totalGoals;
        private final int completedGoals;
        private final int activeGoals;
        private final int overdueGoals;

        public SavingGoalStatsImpl(BigDecimal totalTargetAmount, BigDecimal totalCurrentAmount,
                                  BigDecimal totalProgressPercentage, BigDecimal totalRemainingAmount,
                                  int totalGoals, int completedGoals, int activeGoals, int overdueGoals) {
            this.totalTargetAmount = totalTargetAmount;
            this.totalCurrentAmount = totalCurrentAmount;
            this.totalProgressPercentage = totalProgressPercentage;
            this.totalRemainingAmount = totalRemainingAmount;
            this.totalGoals = totalGoals;
            this.completedGoals = completedGoals;
            this.activeGoals = activeGoals;
            this.overdueGoals = overdueGoals;
        }

        @Override
        public BigDecimal getTotalTargetAmount() {
            return totalTargetAmount;
        }

        @Override
        public BigDecimal getTotalCurrentAmount() {
            return totalCurrentAmount;
        }

        @Override
        public BigDecimal getTotalProgressPercentage() {
            return totalProgressPercentage;
        }

        @Override
        public BigDecimal getTotalRemainingAmount() {
            return totalRemainingAmount;
        }

        @Override
        public int getTotalGoals() {
            return totalGoals;
        }

        @Override
        public int getCompletedGoals() {
            return completedGoals;
        }

        @Override
        public int getActiveGoals() {
            return activeGoals;
        }

        @Override
        public int getOverdueGoals() {
            return overdueGoals;
        }
    }
}
