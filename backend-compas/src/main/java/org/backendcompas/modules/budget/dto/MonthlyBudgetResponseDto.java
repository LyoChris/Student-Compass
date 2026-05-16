package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "MonthlyBudgetResponse",
        description = """
                Complete snapshot of a student's monthly budget, including all category breakdowns,
                live spend tracking, and the dynamically computed Safe-to-Spend metric.

                ## Safe-to-Spend per Day (S2S)
                `safeToSpendPerDay = totalRemaining / remainingDaysInMonth`

                - **Current month:** remaining days = max(1, daysInMonth − today + 1)
                - **Future month:** remaining days = full month length
                - **Past month:** remaining days = 1 (budget is closed)

                `safeToSpendPerDay` is floored at 0 when `totalRemaining ≤ 0`.

                ## Rollover
                When a new month is created, any unspent surplus from the previous month
                (sum of positive `allocatedAmount − spentAmount` across all categories)
                is carried forward as `rolloverAmount` and added to `totalRemaining`.
                """
)
public record MonthlyBudgetResponseDto(

        @Schema(description = "UUID of this monthly budget record.", example = "b1e2f3a4-5678-90ab-cdef-000000000001")
        UUID budgetId,

        @Schema(description = "UUID of the owning student.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "Calendar month (1–12).", example = "5")
        int month,

        @Schema(description = "Four-digit calendar year.", example = "2026")
        int year,

        @Schema(description = "Total declared income for this month in RON.", example = "1500.00")
        BigDecimal totalIncome,

        @Schema(
                description = "Surplus carried over from the previous month in RON. 0.00 if this is the first budget or no surplus existed.",
                example = "85.50"
        )
        BigDecimal rolloverAmount,

        @Schema(description = "Sum of all category `allocatedAmount` values in RON.", example = "1500.00")
        BigDecimal totalAllocated,

        @Schema(description = "Sum of all category `spentAmount` values in RON.", example = "320.40")
        BigDecimal totalSpent,

        @Schema(
                description = "`totalAllocated − totalSpent + rolloverAmount`, floored at 0. Total disposable funds remaining for the month.",
                example = "1265.10"
        )
        BigDecimal totalRemaining,

        @Schema(
                description = "Safe-to-Spend KPI: `totalRemaining / remainingDaysInMonth`, rounded to 2 decimal places.",
                example = "42.17"
        )
        BigDecimal safeToSpendPerDay,

        @Schema(description = "All spending categories in this budget, sorted alphabetically by name.")
        List<BudgetCategoryResponseDto> categories,

        @Schema(description = "UTC timestamp of the last time this budget was modified.", example = "2026-05-16T10:00:00Z")
        Instant updatedAt
) {}
