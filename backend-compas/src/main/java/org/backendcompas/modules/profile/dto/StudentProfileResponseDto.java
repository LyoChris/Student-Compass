package org.backendcompas.modules.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Complete student financial onboarding profile")
public record StudentProfileResponseDto(

        @Schema(
                description = "UUID of the user this profile belongs to — identical to the auth system user ID",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        UUID userId,

        @Schema(description = "The student's current accommodation type", example = "DORMITORY")
        LivingArea livingArea,

        @Schema(description = "The student's primary eating habit", example = "COOKING")
        EatingHabit eatingHabit,

        @Schema(description = "How often the student receives a package from home", example = "MONTHLY")
        HomePackageFrequency homePackageFrequency,

        @Schema(description = "Total monthly budget in RON", example = "1500.00")
        BigDecimal monthlyBudget,

        @Schema(description = "Fixed monthly expenses (rent, subscriptions, etc.), sorted by name")
        List<FixedExpenseDto> fixedExpenses,

        @Schema(description = "Optional dorm UUID — null if not living in a dorm or not selected",
                example = "d0000001-0000-0000-0000-000000000000", nullable = true)
        UUID dormId,

        @Schema(description = "UTC timestamp of when this profile was first created")
        Instant createdAt,

        @Schema(description = "UTC timestamp of the last update to this profile")
        Instant updatedAt
) {
}
