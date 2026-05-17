package org.backendcompas.modules.recommendations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(
        name = "AiRecommendationRequest",
        description = """
                Internal request sent by the Spring Boot API to the Python FastAPI recommendation
                orchestrator. The value is always derived from the authenticated JWT principal; the
                browser never supplies or overrides this identifier.
                """
)
public record AiRecommendationRequestDto(

        @NotNull(message = "userId is required")
        @Schema(
                description = "UUID of the authenticated student for whom recommendations are generated.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID userId
) {}
