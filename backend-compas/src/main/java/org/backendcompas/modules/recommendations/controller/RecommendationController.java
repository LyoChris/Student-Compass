package org.backendcompas.modules.recommendations.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.recommendations.dto.AiChatRequestDto;
import org.backendcompas.modules.recommendations.dto.AiChatResponseDto;
import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;
import org.backendcompas.modules.recommendations.service.AiRecommendationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.TreeSet;

@Tag(
        name = "AI Recommendations & Chat",
        description = """
                AI-powered, budget-aware product recommendations **and** a stateless financial chatbot
                for authenticated students.

                Both endpoints enrich every request with the student's onboarding profile and
                real-time monthly budget before forwarding to the internal Python/FastAPI LLM service.

                **Multi-LLM HA strategy**: if the primary service fails for any reason (timeout,
                5xx, unreachable), the Java layer transparently retries with the Google Gemini API.
                The `source` field in every response tells the frontend which backend answered:
                `llm` = primary, `gemini_fallback` = Google Gemini HA path.

                The internal Python service is protected with `X-Internal-Secret` and strict
                network timeouts (connect 3 s, read 15 s). Gemini is called without timeout
                constraints since it is the last-resort fallback.

                **Chat note:** this endpoint is stateless — messages are not persisted.
                For full conversation history use `POST /api/v1/ai/chat`.
                """
)
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecommendationController {

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "FOOD", "TRANSPORT", "ENTERTAINMENT", "HEALTH", "CLOTHING",
            "EDUCATION", "UTILITIES", "PERSONAL_CARE", "SAVINGS", "OTHER"
    );

    private final AiRecommendationService aiRecommendationService;

    // =========================================================================
    // GET /api/v1/recommendations  — product recommendations
    // =========================================================================

    @Operation(
            summary = "Get context-aware AI product recommendations",
            description = """
                    Returns a ranked, personalised product recommendation list for the requested
                    budget category.

                    **Enrichment pipeline:**
                    1. Extracts `userId` from the JWT-backed `SecurityContext`.
                    2. Loads `StudentProfile` (living area, eating habit, home package frequency).
                       Returns **404** if onboarding is incomplete.
                    3. Resolves `remainingBudget` for the category: `allocated − spent`, clamped ≥ 0.
                    4. POSTs an enriched payload to `{ai.service.url}/api/v1/recommendations`.
                    5. **Fallback:** on any primary service failure, retries against Gemini API.

                    **Caching:** responses are cached by `(userId, CATEGORY, userNote)`.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Recommendations returned (primary LLM or Gemini fallback).",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AiRecommendationResponseDto.class),
                    examples = @ExampleObject(
                            name = "Primary LLM — FOOD",
                            value = """
                                    {
                                      "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "source": "llm",
                                      "recommendations": [
                                        {
                                          "productId": "kaufland-rice-1kg",
                                          "name": "Rice 1kg",
                                          "price": 9.50,
                                          "category": "Groceries",
                                          "storeName": "Kaufland",
                                          "isPartner": true,
                                          "reason": "Low-cost staple ideal for dorm cooking with 120 RON remaining."
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = """
                    `category` is missing, blank, or not one of the accepted values:
                    `FOOD`, `TRANSPORT`, `ENTERTAINMENT`, `HEALTH`, `CLOTHING`,
                    `EDUCATION`, `UTILITIES`, `PERSONAL_CARE`, `SAVINGS`, `OTHER`.
                    """,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid JWT.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Student has not completed onboarding — no `StudentProfile` found.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(value = """
                            {
                              "status": 404,
                              "error": "Not Found",
                              "message": "Student profile not found. Complete onboarding before requesting recommendations.",
                              "path": "/api/v1/recommendations"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "503",
            description = "Both the primary AI service and Gemini fallback failed.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class))
    )
    @GetMapping
    public ResponseEntity<AiRecommendationResponseDto> getRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(
                    name = "category",
                    description = "Budget category for recommendations. Case-insensitive.",
                    example = "FOOD",
                    required = true
            )
            @RequestParam String category,

            @Parameter(
                    name = "userNote",
                    description = "Optional free-text preference hint (max 500 chars), e.g. `I want vegan options`.",
                    example = "I want vegan options only",
                    required = false
            )
            @RequestParam(required = false) String userNote
    ) {
        requireAuthentication(userDetails);

        String normalised = validateCategory(category);

        return ResponseEntity.ok(
                aiRecommendationService.getContextualRecommendations(
                        userDetails.getUserId(), normalised, userNote));
    }

    // =========================================================================
    // POST /api/v1/recommendations/chat  — stateless financial chatbot
    // =========================================================================

    @Operation(
            summary = "Chat with the StuFi AI financial advisor",
            description = """
                    Sends a free-text message to the StuFi AI financial advisor and returns a
                    context-aware reply.

                    **Enrichment pipeline:**
                    1. Loads the student's `StudentProfile` for lifestyle context.
                       Unlike the recommendations endpoint, missing profile does **not** return 404 —
                       the AI replies without profile context instead.
                    2. Computes the student's *total* remaining budget across all categories
                       (`sum(allocated − spent)`, clamped ≥ 0).
                    3. POSTs `{message, context}` to `{ai.service.url}/api/v1/chat`.
                    4. **Fallback:** on any primary service failure, retries against Gemini API
                       using a persona-driven prompt that stays in character as StuFi.

                    **Stateless:** messages are NOT persisted. For a full conversational experience
                    with history, use `POST /api/v1/ai/chat`.

                    The `source` field tells the frontend whether the primary LLM (`llm`) or
                    the Gemini HA path (`gemini_fallback`) produced the reply.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "AI reply returned (primary LLM or Gemini fallback).",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AiChatResponseDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Primary LLM reply",
                                    value = """
                                            {
                                              "reply": "Hey! With 120 RON left for food and 10 days to go, you've got about 12 RON/day. Since you cook at home, batch-cooking pasta or rice on weekends is your best bet. Try Piata Obor for fresh veggies at half the supermarket price!",
                                              "source": "llm"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Gemini fallback reply",
                                    value = """
                                            {
                                              "reply": "No worries! With 120 RON left and 10 days remaining, aim for around 12 RON per day on food. Cooking at home is already saving you a ton — batch cooking is your superpower here.",
                                              "source": "gemini_fallback"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Message is blank or exceeds 2 000 characters.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "message must not be blank"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid JWT.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "503",
            description = "Both the primary AI service and Gemini fallback failed.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class))
    )
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiChatRequestDto request
    ) {
        requireAuthentication(userDetails);

        return ResponseEntity.ok(
                aiRecommendationService.chat(userDetails.getUserId(), request));
    }

    // =========================================================================
    // helpers
    // =========================================================================

    private void requireAuthentication(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Authentication is required");
        }
    }

    private String validateCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("category must not be blank");
        }
        String upper = raw.trim().toUpperCase();
        if (!VALID_CATEGORIES.contains(upper)) {
            throw new BadRequestException(
                    "Unknown budget category: '" + raw + "'. Valid values: "
                    + String.join(", ", new TreeSet<>(VALID_CATEGORIES)));
        }
        return upper;
    }
}
