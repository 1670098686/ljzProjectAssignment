package com.finance.service;

import com.finance.dto.BudgetDto;
import com.finance.dto.CreateBudgetRequest;
import com.finance.dto.UpdateBudgetRequest;

import java.util.List;

public interface BudgetService {

    List<BudgetDto> listBudgets(Integer year, Integer month);

    BudgetDto getBudget(Long id);

    BudgetDto createBudget(CreateBudgetRequest request);

    BudgetDto updateBudget(Long id, UpdateBudgetRequest request);

    void deleteBudget(Long id);
}
