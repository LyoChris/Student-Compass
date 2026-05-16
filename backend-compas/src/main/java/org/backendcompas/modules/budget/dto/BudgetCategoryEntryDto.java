package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.budget.model.BudgetCategory;

import java.math.BigDecimal;

@Schema(
        name = "BudgetCategoryEntry",
        description = """
                A single category allocation inside a budget plan.

                The seven categories are:
                | Category | Typical use |
                |---|---|
                | `FOOD` | Groceries, restaurant, delivery, canteen |
                | `TRANSPORT` | Bus pass, fuel, taxi, ride-sharing |
                | `HOUSING` | Rent, utilities, dorm fee |
                | `SUPPLIES` | Stationery, course materials, printing |
                | `PERSONAL` | Hygiene, clothing, haircuts |
                | `LEISURE` | Entertainment, hobbies, going out |
                | `SAVINGS` | Emergency fund, savings account |
                """
)
public record BudgetCategoryEntryDto(

        @Schema(
                description = "The spending category this allocation belongs to.",
                example = "FOOD",
                allowableValues = {"FOOD", "TRANSPORT", "HOUSING", "SUPPLIES", "PERSONAL", "LEISURE", "SAVINGS"}
        )
        BudgetCategory category,

        @Schema(
                description = "Amount allocated to this category in RON, rounded to 2 decimal places.",
                example = "437.50"
        )
        BigDecimal amount
) {
}
