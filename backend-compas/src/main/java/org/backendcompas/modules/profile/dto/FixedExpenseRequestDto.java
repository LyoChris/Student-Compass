package org.backendcompas.modules.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "A single recurring monthly expense with a label and amount")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpenseRequestDto {

    @Schema(
            description = "Human-readable label for the expense",
            example = "Netflix subscription",
            maxLength = 100
    )
    @NotBlank(message = "Expense name must not be blank")
    @Size(max = 100, message = "Expense name must not exceed 100 characters")
    private String name;

    @Schema(
            description = "Monthly cost of this expense in RON. Must be a positive value with at most 2 decimal places.",
            example = "49.99"
    )
    @NotNull(message = "Expense amount is required")
    @Positive(message = "Expense amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "Expense amount must have at most 2 decimal places")
    private BigDecimal amount;
}
