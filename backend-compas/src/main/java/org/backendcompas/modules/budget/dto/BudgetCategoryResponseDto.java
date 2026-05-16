package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(
        name = "BudgetCategoryResponse",
        description = "One spending category inside a monthly budget, including live spend tracking."
)
public record BudgetCategoryResponseDto(

        @Schema(
                description = "Unique UUID of this category row.",
                example = "c1a2b3d4-0001-0000-0000-000000000001"
        )
        UUID id,

        @Schema(
                description = "Human-readable category name (e.g. FOOD, TRANSPORT, HOUSING, custom names).",
                example = "FOOD"
        )
        String name,

        @Schema(
                description = "Amount of the monthly disposable income allocated to this category in RON.",
                example = "450.00"
        )
        BigDecimal allocatedAmount,

        @Schema(
                description = "Total amount already spent in this category for the month, in RON.",
                example = "120.75"
        )
        BigDecimal spentAmount,

        @Schema(
                description = """
                        `allocatedAmount − spentAmount`, floored at 0.
                        Represents how much budget is still available in this category.
                        """,
                example = "329.25"
        )
        BigDecimal remaining
) {}
