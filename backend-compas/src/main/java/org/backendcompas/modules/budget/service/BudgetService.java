package org.backendcompas.modules.budget.service;

import org.backendcompas.modules.budget.dto.BudgetPlanResponseDto;

import java.util.UUID;

public interface BudgetService {

    BudgetPlanResponseDto getPlan(UUID userId);

    BudgetPlanResponseDto recompute(UUID userId);
}
