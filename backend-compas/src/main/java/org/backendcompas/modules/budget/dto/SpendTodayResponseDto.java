package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "SpendTodayResponse",
        description = """
                Summary of everything the authenticated student has spent today.

                `totalToday` is the sum of all transaction amounts logged for the current
                calendar day (midnight → now). `byCategory` breaks the same total down per
                spending category. `transactions` is the full chronological list of individual
                entries, newest first.
                """
)
public record SpendTodayResponseDto(

        @Schema(description = "The calendar date this summary covers (ISO-8601).", example = "2026-05-17")
        LocalDate date,

        @Schema(description = "Total amount spent today across all categories, in RON.", example = "143.80")
        BigDecimal totalToday,

        @Schema(description = "Per-category breakdown of today's spend, sorted descending by amount.")
        List<CategorySpendDto> byCategory,

        @Schema(description = "Individual transactions logged today, newest first.")
        List<TransactionItemDto> transactions
) {

    @Schema(name = "CategorySpend", description = "Total spend for one category today.")
    public record CategorySpendDto(
            @Schema(example = "FOOD")   String categoryName,
            @Schema(example = "94.30")  BigDecimal amount
    ) {}

    @Schema(name = "TransactionItem", description = "One spending entry.")
    public record TransactionItemDto(
            @Schema(example = "FOOD")                           String categoryName,
            @Schema(example = "47.50")                          BigDecimal amount,
            @Schema(example = "Kaufland — weekly groceries")    String description,
            @Schema(example = "2026-05-17T09:14:32")            String transactionDate
    ) {}
}
