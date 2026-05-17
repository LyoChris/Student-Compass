package org.backendcompas.modules.recommendations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "AiChatRequest",
        description = "A single student message sent to the StuFi AI financial advisor."
)
public record AiChatRequestDto(

        @NotBlank(message = "message must not be blank")
        @Size(max = 2000, message = "message must be at most 2 000 characters")
        @Schema(
                description = "The student's free-text message or question.",
                example = "How much can I spend on food this week if I still want to save 200 RON?",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 2000
        )
        String message
) {}
