package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.profile.dto.FixedExpenseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "BudgetPlanResponse",
        description = """
                Computed monthly spending plan for a student.

                The plan breaks the student's **disposable income** (`monthlyBudget − fixedTotal`)
                into seven spending categories using a deterministic weight matrix that is adjusted
                based on the student's living area, eating habits, and home-package frequency.

                Fixed expenses are passed through verbatim and are **not** re-split into categories
                — they represent known, unavoidable costs (rent, internet, subscriptions, etc.).
                """
)
public record BudgetPlanResponseDto(

        @Schema(
                description = "UUID of this budget plan record.",
                example = "c3d4e5f6-1234-5678-abcd-000000000001"
        )
        UUID planId,

        @Schema(
                description = "UUID of the student this plan belongs to — matches the user's authentication ID.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        UUID userId,

        @Schema(
                description = "Total declared monthly budget in RON, copied directly from the student's profile.",
                example = "1500.00"
        )
        BigDecimal monthlyBudget,

        @Schema(
                description = "Sum of all fixed monthly expenses in RON. Equals the sum of every `amount` in `fixedExpenses`.",
                example = "400.00"
        )
        BigDecimal fixedTotal,

        @Schema(
                description = """
                        Disposable income in RON — the amount available for discretionary spending.
                        Computed as `monthlyBudget − fixedTotal`. Floored at 0 if fixed expenses exceed
                        the total budget.
                        """,
                example = "1100.00"
        )
        BigDecimal disposable,

        @Schema(
                description = """
                        Per-category allocation of `disposable`. Always contains exactly seven entries
                        (FOOD, TRANSPORT, HOUSING, SUPPLIES, PERSONAL, LEISURE, SAVINGS), sorted
                        alphabetically. Amounts sum to `disposable` (rounding errors of ±0.01 RON are
                        possible due to HALF_UP rounding per category).
                        """
        )
        List<BudgetCategoryEntryDto> categories,

        @Schema(
                description = "Fixed monthly expenses passed through verbatim from the student's profile. Not re-split into categories."
        )
        List<FixedExpenseDto> fixedExpenses,

        @Schema(
                description = "UTC timestamp of the last time this plan was (re)computed.",
                example = "2026-05-16T10:00:00Z"
        )
        Instant updatedAt
) {
}
