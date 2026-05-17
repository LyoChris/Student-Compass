package org.backendcompas.modules.recommendations.service;

import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;

import java.util.UUID;

public interface AiRecommendationService {

    /**
     * Fetches context-aware AI recommendations for the given student and spending category.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Load the student's {@code StudentProfile} — throws {@code NotFoundException} (404) if absent.</li>
     *   <li>Load the current month's {@code MonthlyBudget}; use {@code remainingBudget = 0} when none exists.</li>
     *   <li>Resolve the specific category inside the budget; {@code remainingBudget = 0} when not found.</li>
     *   <li>Build an {@code EnrichedAiRequestDto} and forward it to the FastAPI microservice.</li>
     * </ol>
     *
     * <p>Results are cached per {@code (userId, category, userNote)} triple.
     *
     * @param userId    UUID of the authenticated student extracted from the SecurityContext.
     * @param category  Case-insensitive budget category name (e.g. {@code "FOOD"}, {@code "TRANSPORT"}).
     * @param userNote  Optional free-text preference hint (e.g. "vegan only"). May be {@code null}.
     * @return Ranked product recommendations from the AI microservice.
     */
    AiRecommendationResponseDto getContextualRecommendations(UUID userId, String category, String userNote);
}
