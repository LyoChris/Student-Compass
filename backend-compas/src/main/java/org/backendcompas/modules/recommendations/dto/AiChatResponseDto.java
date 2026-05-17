package org.backendcompas.modules.recommendations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "AiChatResponse",
        description = """
                Reply from the StuFi AI financial advisor.

                `source` identifies which AI backend produced the response:
                - `llm` — the primary internal Python/FastAPI service responded successfully.
                - `gemini_fallback` — the primary service was unreachable; Google Gemini was used as HA fallback.
                """
)
public record AiChatResponseDto(

        @Schema(
                description = "AI-generated reply to the student's message.",
                example = "Hey! Based on your remaining food budget of 120 RON and 12 days left in the month, you can spend around 10 RON/day on food. That's totally doable if you cook at home — try batch-cooking rice and beans this weekend!"
        )
        String reply,

        @Schema(
                description = "Identifies which AI backend produced this response.",
                example = "llm",
                allowableValues = {"llm", "gemini_fallback"}
        )
        String source
) {}
