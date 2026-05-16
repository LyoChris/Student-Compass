package org.backendcompas.modules.budget.repository;

import org.backendcompas.modules.budget.model.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, UUID> {

    Optional<MonthlyBudget> findByUserIdAndMonthAndYear(UUID userId, int month, int year);
}
