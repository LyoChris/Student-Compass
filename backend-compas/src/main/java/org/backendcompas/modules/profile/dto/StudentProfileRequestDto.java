package org.backendcompas.modules.profile.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Request body for creating or updating a student's financial onboarding profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileRequestDto {

    @Schema(
            description = "The student's current accommodation type",
            example = "DORMITORY",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Living area is required")
    private LivingArea livingArea;

    @Schema(
            description = "The student's primary eating habit",
            example = "COOKING",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Eating habit is required")
    private EatingHabit eatingHabit;

    @Schema(
            description = "How often the student receives a package from home",
            example = "MONTHLY",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Home package frequency is required")
    private HomePackageFrequency homePackageFrequency;

    @Schema(
            description = "Total monthly budget in RON. Must be a positive value with at most 2 decimal places.",
            example = "1500.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Monthly budget is required")
    @Positive(message = "Monthly budget must be positive")
    @Digits(integer = 10, fraction = 2, message = "Monthly budget must have at most 2 decimal places")
    private BigDecimal monthlyBudget;

    @ArraySchema(
            schema = @Schema(implementation = FixedExpenseRequestDto.class),
            arraySchema = @Schema(
                    description = "List of fixed monthly expenses (rent, internet, subscriptions, etc.). "
                            + "Send an empty list if there are no fixed expenses.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
    )
    @NotNull(message = "Fixed expenses list is required — send an empty list if there are none")
    @Valid
    @Builder.Default
    private List<FixedExpenseRequestDto> fixedExpenses = new ArrayList<>();
}
