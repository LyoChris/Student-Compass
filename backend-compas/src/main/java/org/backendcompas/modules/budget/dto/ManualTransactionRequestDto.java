package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(
        name = "ManualTransactionRequest",
        description = """
                Payload for recording a manual spending entry against a budget category.

                The service will:
                1. Look up the `budgetId` and verify the authenticated user owns it.
                2. Find the category by `categoryName` (case-insensitive) inside that budget.
                3. Create a `transactions` row with the supplied `amount` and `description`.
                4. Increment `budget_categories.spent_amount` by `amount`.

                Returns 404 if either the budget or the named category does not exist.
                """
)
public record ManualTransactionRequestDto(

        @Schema(
                description = "UUID of the monthly budget this transaction belongs to.",
                example = "b1e2f3a4-5678-90ab-cdef-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "budgetId is required")
        UUID budgetId,

        @Schema(
                description = "Category name to charge, matched case-insensitively. Must already exist in the budget.",
                example = "FOOD",
                minLength = 1,
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "categoryName must not be blank")
        @Size(max = 50, message = "categoryName must be at most 50 characters")
        String categoryName,

        @Schema(
                description = "Positive amount spent in RON.",
                example = "47.50",
                exclusiveMinimum = true,
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @Schema(
                description = "Optional free-text description of the purchase (receipt merchant, notes, etc.).",
                example = "Kaufland — weekly groceries",
                maxLength = 255
        )
        @Size(max = 255, message = "description must be at most 255 characters")
        String description
) {}
