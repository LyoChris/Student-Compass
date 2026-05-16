package org.backendcompas.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "ChatResponse",
        description = """
                AI coach reply returned after a `POST /api/v1/ai/chat` request.

                Both the student's message and this reply are persisted in `chat_messages` and
                will appear in the history endpoint. If the AI service was temporarily unavailable,
                `reply` contains a human-readable fallback message — the HTTP status is still 200.
                """
)
public record ChatResponseDto(

        @Schema(
                description = "UUID of the persisted `ASSISTANT` message. Can be used to reference this specific reply.",
                example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34"
        )
        UUID messageId,

        @Schema(
                description = """
                        The AI-generated reply text. Normally a personalised financial coaching answer
                        based on the student's profile and budget context.

                        If the AI service was unreachable at the time of the request, this field contains
                        the fallback message: _"Sorry, the AI coach is temporarily unavailable. Please
                        try again later."_
                        """,
                example = "Based on your profile you have 315 RON allocated for food this month. Since you cook at home that's roughly 10 RON per day — try shopping at the Piata Centrala for better prices."
        )
        String reply
) {
}
