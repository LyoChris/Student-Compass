package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        name = "ParsedTransaction",
        description = """
                One row parsed from an uploaded bank statement CSV.

                `persisted = true` means the transaction was successfully matched to an existing
                budget category and saved to the `transactions` table. `persisted = false` means
                the keyword routing could not find a matching category in the budget — the user
                should add the category via `PUT /{budgetId}/categories` and re-upload, or log it
                manually via `POST /transactions`.
                """
)
public record ParsedTransactionDto(

        @Schema(description = "Raw description / merchant name extracted from the CSV row.", example = "KAUFLAND IASI 2")
        String description,

        @Schema(description = "Absolute transaction amount in RON as parsed from the CSV.", example = "94.32")
        BigDecimal amount,

        @Schema(
                description = """
                        Category name the keyword router assigned to this row.
                        Common auto-detected values: `FOOD`, `TRANSPORT`, `HOUSING`, `PERSONAL`, `LEISURE`, `SUPPLIES`.
                        Falls back to `PERSONAL` when no keyword matches.
                        """,
                example = "FOOD"
        )
        String detectedCategory,

        @Schema(
                description = """
                        `true` if a matching category existed in the budget and the transaction was
                        saved + the category's `spentAmount` was updated. `false` if the category
                        did not exist and the row was skipped.
                        """,
                example = "true"
        )
        boolean persisted
) {}
