package org.backendcompas.modules.budget.service;

import org.backendcompas.modules.budget.dto.CategoryAdjustDto;
import org.backendcompas.modules.budget.dto.ManualTransactionRequestDto;
import org.backendcompas.modules.budget.dto.MonthlyBudgetResponseDto;
import org.backendcompas.modules.budget.dto.ParsedTransactionDto;
import org.backendcompas.modules.budget.dto.SpendTodayResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BudgetService {

    /**
     * Returns the existing budget for (userId, month, year), creating it on first access.
     *
     * Creation strategy:
     *  1. If previous month exists → copy its categories as template + compute rollover surplus.
     *  2. Otherwise → generate default allocations from the student's profile weight matrix,
     *     falling back to hardcoded standard weights when no profile is found.
     */
    MonthlyBudgetResponseDto getOrCreateBudget(UUID userId, int month, int year);

    /**
     * Upserts a category inside a budget. Creates it if it doesn't exist, updates allocation
     * if it does. Throws ForbiddenException if userId != budget owner.
     */
    void adjustCategory(UUID userId, UUID budgetId, CategoryAdjustDto dto);

    /**
     * Removes a category (and all its transactions via CASCADE) from a budget.
     * Throws NotFoundException if the named category doesn't exist.
     * Throws ForbiddenException if userId != budget owner.
     */
    void deleteCategory(UUID userId, UUID budgetId, String categoryName);

    /**
     * Logs a manual spending entry: creates a transaction row and increments
     * budget_categories.spent_amount by dto.amount.
     * Throws ForbiddenException if userId != budget owner.
     */
    void logManualTransaction(UUID userId, ManualTransactionRequestDto dto);

    /**
     * Parses a CSV bank statement (Revolut/ING format), keyword-routes each debit row
     * to a category, persists matched transactions, and returns per-row results.
     * Rows that don't match any existing category are returned with persisted=false.
     * Throws ForbiddenException if userId != budget owner.
     */
    List<ParsedTransactionDto> parseBankStatement(UUID userId, UUID budgetId, MultipartFile file);

    /**
     * Returns a summary of all transactions the user logged today (midnight → now):
     * total amount, per-category breakdown, and the individual entries newest-first.
     */
    SpendTodayResponseDto getSpendToday(UUID userId);
}
