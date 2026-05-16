package org.backendcompas.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ChatRequest",
        description = """
                Message payload sent by a student to the AI finance coach.

                The backend automatically enriches the request with the student's profile and
                budget plan before forwarding it to the AI service — no extra fields are needed
                from the client.
                """
)
public record ChatRequestDto(

        @Schema(
                description = """
                        The student's message to the AI coach. Must be non-blank and at most 4 000
                        characters. Markdown is supported but not required.
                        """,
                example = "How much should I spend on groceries this month?",
                minLength = 1,
                maxLength = 4000,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Message must not be blank")
        @Size(max = 4000, message = "Message must be at most 4000 characters")
        String message
) {
}
