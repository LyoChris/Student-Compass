package org.backendcompas.modules.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.ai.dto.ChatMessageDto;
import org.backendcompas.modules.ai.dto.ChatRequestDto;
import org.backendcompas.modules.ai.dto.ChatResponseDto;
import org.backendcompas.modules.ai.service.AiChatService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "AI Chat",
        description = """
                AI-powered finance coach that helps students manage their money.

                ## Architecture
                The browser **never** calls the AI service directly. All messages flow through the
                Spring backend, which:
                1. Enriches every request with the student's **financial profile** and **budget plan**
                   as context (so the AI already knows the student's living situation, eating habits,
                   and monthly allocations without the student having to repeat them).
                2. Forwards the enriched payload — including the last 20 messages as conversation
                   history — to the private **FastAPI AI service** via a shared-secret header
                   (`X-Internal-Secret`).
                3. Persists both the student's message and the AI reply in `chat_messages` so
                   history is available across sessions.

                ## Fallback behaviour
                If the AI service is temporarily unavailable (network error, timeout, 5xx), the
                backend returns a graceful fallback reply instead of a 5xx error, so the UI remains
                functional. The exchange is **not** persisted when the AI service fails.

                ## Rate & length limits
                Each message is capped at **4 000 characters**. History is capped at the last
                **20 messages** sent to the model, though more can be retrieved via the history
                endpoint.
                """
)
@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    // -------------------------------------------------------------------------
    // POST /
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Send a message to the AI finance coach",
            description = """
                    Submits a student message to the Claude-powered AI finance coach.

                    **What gets sent to the AI:**
                    ```json
                    {
                      "context": {
                        "profile": { "livingArea": "DORMITORY", "eatingHabit": "COOKING", ... },
                        "plan":    { "disposable": 900.00, "categories": [ ... ] }
                      },
                      "history": [ { "role": "USER", "content": "..." }, ... ],
                      "message": "<student message>"
                    }
                    ```
                    The `context` fields are omitted if the student has not yet completed onboarding
                    (the AI will still reply, but without personalisation).

                    **Persistence:**
                    After a successful AI response, both the student's message and the AI reply are
                    saved to `chat_messages` with the authenticated user's ID.

                    **Fallback:**
                    If the AI service is unreachable, the endpoint still returns HTTP 200 with a
                    human-readable fallback message — it does **not** return 5xx.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @RequestBody(
                    description = "The student's message. Must be non-blank and at most 4 000 characters.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Budget question",
                                            summary = "Asking about food allocation",
                                            value = """
                                                    { "message": "How much should I spend on groceries this month?" }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Savings advice",
                                            summary = "Asking for savings tips",
                                            value = """
                                                    { "message": "I have 1500 RON/month and want to save more. Any tips?" }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponse(
                    responseCode = "200",
                    description = "AI reply returned and exchange persisted. If the AI service was unreachable, a fallback reply is returned instead of an error.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Normal reply",
                                            value = """
                                                    {
                                                      "messageId": "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34",
                                                      "reply": "Based on your profile you have 315 RON allocated for food this month. Since you cook at home, that should cover roughly 3–4 meals a day if you shop at local markets."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Fallback reply (AI service down)",
                                            value = """
                                                    {
                                                      "messageId": "a1b2c3d4-0000-0000-0000-000000000001",
                                                      "reply": "Sorry, the AI coach is temporarily unavailable. Please try again later."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — message is blank or exceeds 4 000 characters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Blank message",
                                            value = """
                                                    { "status": 400, "error": "Bad Request", "message": "message: Message must not be blank" }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Message too long",
                                            value = """
                                                    { "status": 400, "error": "Bad Request", "message": "message: Message must be at most 4000 characters" }
                                                    """
                                    )
                            }
                    )
            )
    @ApiResponse(
                    responseCode = "401",
                    description = "No valid Bearer token provided in the `Authorization` header",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 401, "error": "Unauthorized", "message": "Full authentication is required" }
                                    """)
                    )
            )
    @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error (distinct from the AI service being unavailable — that is a 200 fallback)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(aiChatService.chat(principal.getUserId(), request));
    }

    // -------------------------------------------------------------------------
    // GET /history
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get chat history",
            description = """
                    Returns the most recent messages for the **authenticated user**, ordered newest
                    first (descending `created_at`).

                    The response is a Spring `Page` object even though only a single page is ever
                    returned — the `totalElements` field tells the client the total number of stored
                    messages, which can be used to show a "load more" indicator.

                    Use the `limit` query parameter to control how many messages to retrieve
                    (default: **20**, max: **100**). Values outside this range are clamped silently.

                    **Access rules:** A student can only retrieve their **own** history — the user
                    ID is always taken from the JWT, not from a request parameter.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of messages returned (newest first). Empty page if the student has no messages yet.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ChatMessageDto.class)),
                            examples = @ExampleObject(
                                    name = "Two-message history",
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "id": "a1b2c3d4-0000-0000-0000-000000000002",
                                                  "role": "ASSISTANT",
                                                  "content": "Based on your profile you have 315 RON allocated for food.",
                                                  "createdAt": "2026-05-16T11:30:05Z"
                                                },
                                                {
                                                  "id": "a1b2c3d4-0000-0000-0000-000000000001",
                                                  "role": "USER",
                                                  "content": "How much should I spend on groceries?",
                                                  "createdAt": "2026-05-16T11:30:00Z"
                                                }
                                              ],
                                              "totalElements": 2,
                                              "totalPages": 1,
                                              "size": 20,
                                              "number": 0,
                                              "last": true
                                            }
                                            """
                            )
                    )
            )
    @ApiResponse(
                    responseCode = "401",
                    description = "No valid Bearer token provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 401, "error": "Unauthorized", "message": "Full authentication is required" }
                                    """)
                    )
            )
    @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    @GetMapping("/history")
    public ResponseEntity<Page<ChatMessageDto>> getHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(
                    description = "Maximum number of messages to return. Accepted range: 1–100. Values outside this range are clamped silently.",
                    example = "20",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
            )
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(aiChatService.getHistory(principal.getUserId(), limit));
    }
}
