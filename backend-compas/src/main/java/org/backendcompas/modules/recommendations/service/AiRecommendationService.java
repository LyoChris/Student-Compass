package org.backendcompas.modules.recommendations.service;

import org.backendcompas.modules.recommendations.dto.AiChatRequestDto;
import org.backendcompas.modules.recommendations.dto.AiChatResponseDto;
import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;

import java.util.UUID;

public interface AiRecommendationService {

    /**
     * Fetches context-aware AI product recommendations for the given student and spending category.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Load the student's {@code StudentProfile} — throws {@code NotFoundException} (404) if absent.</li>
     *   <li>Load the current month's {@code MonthlyBudget}; use {@code remainingBudget = 0} when none exists.</li>
     *   <li>Resolve the specific category inside the budget; {@code remainingBudget = 0} when not found.</li>
     *   <li>POST an enriched payload to the FastAPI microservice; fall back to Gemini on any failure.</li>
     * </ol>
     *
     * <p>Results are cached per {@code (userId, category, userNote)} triple.
     *
     * @param userId    UUID of the authenticated student extracted from the SecurityContext.
     * @param category  Case-normalised budget category name (e.g. {@code "FOOD"}).
     * @param userNote  Optional free-text preference hint. May be {@code null}.
     */
    AiRecommendationResponseDto getContextualRecommendations(UUID userId, String category, String userNote);

    /**
     * Sends a free-text message to the StuFi AI financial advisor and returns a reply.
     *
     * <p>The method enriches the request with the student's onboarding profile and their total
     * remaining budget for the current month before forwarding it to the primary AI service.
     * If the primary service fails for any reason, the call is transparently retried against
     * the Google Gemini API. Only throws if both services fail.
     *
     * <p>This is a stateless endpoint — messages are not persisted. For a full persistent
     * conversation experience (with history) use {@code POST /api/v1/ai/chat}.
     *
     * @param userId  UUID of the authenticated student.
     * @param request Validated DTO containing the student's message.
     */
    AiChatResponseDto chat(UUID userId, AiChatRequestDto request);
}
