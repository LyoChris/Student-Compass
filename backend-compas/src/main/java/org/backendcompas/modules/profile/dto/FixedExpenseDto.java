package org.backendcompas.modules.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "A single fixed monthly expense returned in the profile response")
public record FixedExpenseDto(

        @Schema(description = "Human-readable label for the expense", example = "Netflix subscription")
        String name,

        @Schema(description = "Monthly cost of this expense in RON", example = "49.99")
        BigDecimal amount
) {
}
