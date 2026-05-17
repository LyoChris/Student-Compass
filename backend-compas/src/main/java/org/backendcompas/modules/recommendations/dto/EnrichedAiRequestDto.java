package org.backendcompas.modules.recommendations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Enriched payload sent from Spring Boot to the Python/FastAPI recommendation microservice.
 *
 * Contract: POST {ai.service.url}/api/v1/recommendations
 */
@Schema(
        name = "EnrichedAiRequest",
        description = """
                Context-rich payload forwarded to the internal FastAPI AI orchestrator.
                Carries the student's identity, the requested spending category, their remaining
                budget for that category this month, and key lifestyle signals extracted from
                the onboarding profile. The Python service uses this context to produce
                personalised, budget-aware product recommendations.
                """
)
public record EnrichedAiRequestDto(

        @NotNull
        @Schema(
                description = "UUID of the authenticated student.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("userId")
        UUID userId,

        @NotBlank
        @Schema(
                description = "Budget category for which recommendations are requested (case-normalised to UPPER_SNAKE_CASE).",
                example = "FOOD",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("category")
        String category,

        @NotNull
        @DecimalMin(value = "0.0", message = "remainingBudget must be >= 0")
        @Schema(
                description = """
                        Amount remaining in the requested category for the current month, in RON.
                        Computed as `allocated_amount - spent_amount`. Set to 0.00 when the
                        student has no budget record or the category does not exist yet.
                        """,
                example = "120.50",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("remainingBudget")
        BigDecimal remainingBudget,

        @Size(max = 500, message = "userNote must not exceed 500 characters")
        @Schema(
                description = """
                        Optional free-text preference hint supplied by the student
                        (e.g. "I want vegan options only", "gluten-free please").
                        Forwarded verbatim to the AI service to steer the LLM output.
                        Null when the student provides no hint.
                        """,
                example = "I want vegan options only",
                nullable = true,
                maxLength = 500
        )
        @JsonProperty("userNote")
        String userNote,

        @NotNull
        @Valid
        @Schema(
                description = "Lifestyle signals extracted from the student's onboarding profile.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("profileContext")
        ProfileContext profileContext
) {

    /**
     * Nested lifestyle context block sourced directly from {@code StudentProfile}.
     */
    @Schema(
            name = "ProfileContext",
            description = "Lifestyle signals from the student's onboarding profile used to personalise recommendations."
    )
    public record ProfileContext(

            @NotNull
            @Schema(
                    description = "Where the student currently lives. Drives cost tier and proximity logic in the AI layer.",
                    example = "DORMITORY",
                    allowableValues = {"DORMITORY", "RENT", "OWN_HOME", "COMMUTER"},
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @JsonProperty("livingArea")
            LivingArea livingArea,

            @NotNull
            @Schema(
                    description = "Primary food procurement habit. Used to rank food-related recommendations appropriately.",
                    example = "COOKING",
                    allowableValues = {"COOKING", "CANTEEN", "DELIVERY", "EATING_OUT"},
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @JsonProperty("eatingHabit")
            EatingHabit eatingHabit,

            @NotNull
            @Schema(
                    description = "How often the student receives a home package. Affects grocery/household recommendation density.",
                    example = "WEEKLY",
                    allowableValues = {"WEEKLY", "BI_WEEKLY", "MONTHLY", "NONE"},
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @JsonProperty("homePackageFrequency")
            HomePackageFrequency homePackageFrequency
    ) {}
}
