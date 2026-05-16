package org.backendcompas.modules.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(
        name = "BudgetCreateRequest",
        description = """
                Parameters for creating or retrieving a monthly budget envelope.
                Submitted as query parameters on `GET /api/v1/budgets/current`.

                When no prior budget exists for the requested month the system will
                automatically generate one using either the previous month as a template
                (with surplus rollover) or the student's profile-derived weight matrix.
                """
)
public record BudgetCreateRequestDto(

        @Schema(
                description = "Calendar month (1 = January … 12 = December).",
                example = "5",
                minimum = "1",
                maximum = "12",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "month is required")
        @Min(value = 1, message = "month must be between 1 and 12")
        @Max(value = 12, message = "month must be between 1 and 12")
        Integer month,

        @Schema(
                description = "Four-digit calendar year. Must be 2020 or later.",
                example = "2026",
                minimum = "2020",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "year is required")
        @Min(value = 2020, message = "year must be 2020 or later")
        Integer year,

        @Schema(
                description = """
                        Total declared income for this month in RON.
                        Used only when creating a brand-new budget (no previous month template).
                        On subsequent calls for the same month this value is ignored — the stored
                        income is returned as-is.
                        """,
                example = "1500.00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Positive(message = "totalIncome must be positive")
        BigDecimal totalIncome
) {}
