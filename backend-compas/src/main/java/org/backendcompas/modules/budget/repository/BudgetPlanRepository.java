package org.backendcompas.modules.budget.repository;

import org.backendcompas.modules.budget.model.BudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, UUID> {

    Optional<BudgetPlan> findByUserId(UUID userId);
}
