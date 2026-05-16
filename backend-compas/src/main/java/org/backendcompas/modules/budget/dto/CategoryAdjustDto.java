package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "CategoryAdjust",
        description = """
                Payload for adding a new category or updating the allocated amount of an existing
                category inside a monthly budget.

                **Upsert semantics:** if a category with `categoryName` already exists, only
                `newAllocation` is updated. If it does not exist, a new category row is created
                with `spentAmount = 0`.

                Category names are matched **case-insensitively** so "food" and "FOOD" refer to
                the same category.
                """
)
public record CategoryAdjustDto(

        @Schema(
                description = """
                        Human-readable category name, 1–50 characters.
                        Common built-in names used by the auto-generated template:
                        `FOOD`, `TRANSPORT`, `HOUSING`, `SUPPLIES`, `PERSONAL`, `LEISURE`, `SAVINGS`.
                        Custom names (e.g. `HOBBY`, `GYM`, `SUBSCRIPTIONS`) are also accepted.
                        """,
                example = "FOOD",
                minLength = 1,
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "categoryName must not be blank")
        @Size(max = 50, message = "categoryName must be at most 50 characters")
        String categoryName,

        @Schema(
                description = "New allocated amount for this category in RON. Must be ≥ 0.00.",
                example = "400.00",
                minimum = "0.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "newAllocation is required")
        @DecimalMin(value = "0.00", message = "newAllocation must be 0.00 or greater")
        BigDecimal newAllocation
) {}
