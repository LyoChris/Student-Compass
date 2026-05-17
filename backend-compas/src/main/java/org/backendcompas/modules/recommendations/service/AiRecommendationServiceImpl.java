package org.backendcompas.modules.recommendations.service;

import lombok.extern.slf4j.Slf4j;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.budget.model.MonthlyBudget;
import org.backendcompas.modules.budget.repository.MonthlyBudgetRepository;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;
import org.backendcompas.modules.recommendations.dto.EnrichedAiRequestDto;
import org.backendcompas.modules.recommendations.exception.AiServiceTimeoutException;
import org.backendcompas.modules.recommendations.exception.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final RestClient aiRestClient;
    private final StudentProfileRepository profileRepository;
    private final MonthlyBudgetRepository budgetRepository;

    public AiRecommendationServiceImpl(
            @Qualifier("aiRestClient") RestClient aiRestClient,
            StudentProfileRepository profileRepository,
            MonthlyBudgetRepository budgetRepository
    ) {
        this.aiRestClient = aiRestClient;
        this.profileRepository = profileRepository;
        this.budgetRepository = budgetRepository;
    }

    @Override
    @Cacheable(
            value = "ai_recommendations",
            key = "#userId + '_' + #category.toUpperCase() + '_' + (#userNote != null ? #userNote.trim() : 'NONE')"
    )
    public AiRecommendationResponseDto getContextualRecommendations(UUID userId, String category, String userNote) {
        String normalizedCategory = category.trim().toUpperCase();
        log.info("Building enriched AI recommendation request: userId={}, category={}, hasUserNote={}",
                userId, normalizedCategory, userNote != null);

        // 1. Profile — mandatory; 404 if the student hasn't completed onboarding
        StudentProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("AI recommendations requested but no profile found: userId={}", userId);
                    return new NotFoundException(
                            "Student profile not found. Please complete onboarding before requesting recommendations.");
                });

        // 2. Budget — optional; gracefully falls back to 0.00 remaining
        LocalDate today = LocalDate.now();
        BigDecimal remainingBudget = budgetRepository
                .findByUserIdAndMonthAndYear(userId, today.getMonthValue(), today.getYear())
                .map(budget -> resolveRemaining(budget, normalizedCategory, userId))
                .orElseGet(() -> {
                    log.debug("No monthly budget for userId={} month={}/{} — using remainingBudget=0",
                            userId, today.getMonthValue(), today.getYear());
                    return BigDecimal.ZERO;
                });

        // 3. Build enriched payload
        String sanitizedNote = (userNote != null && !userNote.isBlank()) ? userNote.trim() : null;

        EnrichedAiRequestDto enrichedRequest = new EnrichedAiRequestDto(
                userId,
                normalizedCategory,
                remainingBudget,
                sanitizedNote,
                new EnrichedAiRequestDto.ProfileContext(
                        profile.getLivingArea(),
                        profile.getEatingHabit(),
                        profile.getHomePackageFrequency()
                )
        );

        log.info("Forwarding enriched request to AI service: userId={}, category={}, remainingBudget={}",
                userId, normalizedCategory, remainingBudget);

        return callAiService(enrichedRequest, userId, normalizedCategory);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Finds the requested category inside the budget and computes remaining = allocated - spent.
     * Returns 0.00 if the category name is not present in the budget (category not yet tracked).
     */
    private BigDecimal resolveRemaining(MonthlyBudget budget, String normalizedCategory, UUID userId) {
        return budget.getCategories().stream()
                .filter(c -> c.getName().equalsIgnoreCase(normalizedCategory))
                .findFirst()
                .map(c -> {
                    BigDecimal remaining = c.getAllocatedAmount().subtract(c.getSpentAmount());
                    // Clamp to 0 — negative remaining means overspent, AI should know budget is exhausted
                    BigDecimal clamped = remaining.max(BigDecimal.ZERO);
                    log.debug("Category '{}' found in budget for userId={}: allocated={}, spent={}, remaining={}",
                            normalizedCategory, userId, c.getAllocatedAmount(), c.getSpentAmount(), clamped);
                    return clamped;
                })
                .orElseGet(() -> {
                    log.debug("Category '{}' not tracked in budget for userId={} — using remainingBudget=0",
                            normalizedCategory, userId);
                    return BigDecimal.ZERO;
                });
    }

    /**
     * Executes the HTTP call to the FastAPI microservice with structured error handling.
     * Maps every failure mode to the appropriate typed exception so the GlobalExceptionHandler
     * can produce consistent HTTP responses for the frontend.
     */
    private AiRecommendationResponseDto callAiService(
            EnrichedAiRequestDto request, UUID userId, String category) {
        try {
            AiRecommendationResponseDto response = aiRestClient.post()
                    .uri("/api/v1/recommendations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiRecommendationResponseDto.class);

            if (response == null) {
                log.error("AI service returned null body: userId={}, category={}", userId, category);
                throw new AiServiceUnavailableException("AI recommendations service returned an empty response");
            }

            log.info("AI service responded: userId={}, category={}, source={}, items={}",
                    userId, category, response.source(), response.recommendations().size());
            return response;

        } catch (ResourceAccessException ex) {
            log.error("AI service timeout or unreachable: userId={}, category={} — {}",
                    userId, category, ex.getMessage(), ex);
            throw new AiServiceTimeoutException("AI recommendations service timed out");

        } catch (RestClientResponseException ex) {
            log.error("AI service HTTP error {}: userId={}, category={}, body={}",
                    ex.getStatusCode(), userId, category, ex.getResponseBodyAsString(), ex);
            throw new AiServiceUnavailableException("AI recommendations service is temporarily unavailable");

        } catch (RestClientException ex) {
            log.error("AI service call failed: userId={}, category={} — {}", userId, category, ex.getMessage(), ex);
            throw new AiServiceUnavailableException("AI recommendations service is temporarily unavailable");

        } catch (AiServiceTimeoutException | AiServiceUnavailableException ex) {
            // Re-throw typed exceptions so they are not swallowed by the generic handler below
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected failure requesting AI recommendations: userId={}, category={} — {}",
                    userId, category, ex.getMessage(), ex);
            throw new AiServiceUnavailableException("AI recommendations service failed unexpectedly");
        }
    }
}
