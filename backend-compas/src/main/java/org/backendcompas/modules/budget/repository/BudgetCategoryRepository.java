package org.backendcompas.modules.budget.repository;

import org.backendcompas.modules.budget.model.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, UUID> {

    Optional<BudgetCategory> findByBudgetIdAndNameIgnoreCase(UUID budgetId, String name);
}
