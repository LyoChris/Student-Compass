package org.backendcompas.modules.recommendations.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "AiRecommendationResponse",
        description = """
                Response returned by the Spring Boot recommendations endpoint after calling the
                internal Python/FastAPI AI orchestrator. The `source` field tells the frontend
                whether the list came from the LLM pipeline or from the Python service's deterministic
                fallback path.
                """
)
public record AiRecommendationResponseDto(

        @NotNull(message = "userId is required")
        @Schema(
                description = "UUID of the authenticated student whose preferences and budget context were used.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID userId,

        @NotBlank(message = "source is required")
        @Pattern(regexp = "llm|fallback|gemini_fallback", message = "source must be llm, fallback, or gemini_fallback")
        @Schema(
                description = "`llm` — primary Ollama path; `fallback` — Python service deterministic backup; `gemini_fallback` — Google Gemini HA fallback activated when the primary service was unreachable.",
                example = "llm",
                allowableValues = {"llm", "fallback", "gemini_fallback"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String source,

        @Valid
        @NotEmpty(message = "recommendations must contain at least one product")
        @Size(max = 50, message = "recommendations must contain at most 50 products")
        @ArraySchema(
                schema = @Schema(implementation = AiProductDto.class),
                minItems = 1,
                maxItems = 50,
                arraySchema = @Schema(
                        description = "Ranked product recommendations. The first item is the strongest match for the student's current context.",
                        requiredMode = Schema.RequiredMode.REQUIRED
                )
        )
        List<AiProductDto> recommendations
) {}
