package org.backendcompas.modules.recommendations.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.budget.model.BudgetCategory;
import org.backendcompas.modules.budget.model.MonthlyBudget;
import org.backendcompas.modules.budget.repository.MonthlyBudgetRepository;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.backendcompas.modules.recommendations.dto.AiChatRequestDto;
import org.backendcompas.modules.recommendations.dto.AiChatResponseDto;
import org.backendcompas.modules.recommendations.dto.AiProductDto;
import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;
import org.backendcompas.modules.recommendations.dto.EnrichedAiRequestDto;
import org.backendcompas.modules.recommendations.exception.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AiRecommendationServiceImpl implements AiRecommendationService {

    // ── constants ─────────────────────────────────────────────────────────────

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_PATH     = "/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String SOURCE_LLM      = "llm";
    private static final String SOURCE_GEMINI   = "gemini_fallback";

    // ── Gemini API wire types (internal — never serialised to callers) ────────

    private record GeminiPart(String text) {}
    private record GeminiContent(List<GeminiPart> parts) {}
    private record GeminiRequest(List<GeminiContent> contents) {}
    private record GeminiCandidate(GeminiContent content) {}
    private record GeminiResponse(List<GeminiCandidate> candidates) {}

    // ── Python chat wire types ────────────────────────────────────────────────

    private record PythonChatRequest(String message, String context) {}
    private record PythonChatResponse(String reply) {}

    // ── dependencies ─────────────────────────────────────────────────────────

    private final RestClient              aiRestClient;
    private final RestClient              geminiRestClient;
    private final StudentProfileRepository profileRepository;
    private final MonthlyBudgetRepository  budgetRepository;
    private final ObjectMapper             objectMapper;
    private final String                   geminiApiKey;

    public AiRecommendationServiceImpl(
            @Qualifier("aiRestClient") RestClient aiRestClient,
            StudentProfileRepository profileRepository,
            MonthlyBudgetRepository budgetRepository,
            @Value("${gemini.api.key}") String geminiApiKey
    ) {
        this.aiRestClient      = aiRestClient;
        this.profileRepository = profileRepository;
        this.budgetRepository  = budgetRepository;
        this.geminiApiKey      = geminiApiKey;
        // ObjectMapper is thread-safe once built; avoid the Spring bean wiring issue by constructing directly.
        this.objectMapper      = new ObjectMapper();
        this.geminiRestClient  = RestClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .build();
    }

    // =========================================================================
    // PUBLIC API — RECOMMENDATIONS
    // =========================================================================

    @Override
    @Cacheable(
            value = "ai_recommendations",
            key  = "#userId + '_' + #category.toUpperCase() + '_' + (#userNote != null ? #userNote.trim() : 'NONE')"
    )
    public AiRecommendationResponseDto getContextualRecommendations(
            UUID userId, String category, String userNote) {

        String normalisedCategory = category.trim().toUpperCase();
        log.info("Recommendations request: userId={}, category={}, hasUserNote={}",
                userId, normalisedCategory, userNote != null);

        // 1. Profile mandatory — 404 if onboarding incomplete
        StudentProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("No profile found for recommendations: userId={}", userId);
                    return new NotFoundException(
                            "Student profile not found. Complete onboarding before requesting recommendations.");
                });

        // 2. Category remaining budget (0 when no budget or category not tracked yet)
        LocalDate today = LocalDate.now();
        BigDecimal remainingBudget = budgetRepository
                .findByUserIdAndMonthAndYear(userId, today.getMonthValue(), today.getYear())
                .map(b -> resolveCategoryRemaining(b, normalisedCategory, userId))
                .orElseGet(() -> {
                    log.debug("No budget for userId={} {}/{} — remainingBudget=0",
                            userId, today.getMonthValue(), today.getYear());
                    return BigDecimal.ZERO;
                });

        // 3. Build enriched payload
        String sanitisedNote = (userNote != null && !userNote.isBlank()) ? userNote.trim() : null;

        EnrichedAiRequestDto enriched = new EnrichedAiRequestDto(
                userId,
                normalisedCategory,
                remainingBudget,
                sanitisedNote,
                new EnrichedAiRequestDto.ProfileContext(
                        profile.getLivingArea(),
                        profile.getEatingHabit(),
                        profile.getHomePackageFrequency()
                )
        );

        log.info("Forwarding recommendations to AI: userId={}, category={}, remaining={}",
                userId, normalisedCategory, remainingBudget);

        return callRecommendationsWithFallback(enriched, userId, normalisedCategory);
    }

    // =========================================================================
    // PUBLIC API — CHAT
    // =========================================================================

    @Override
    public AiChatResponseDto chat(UUID userId, AiChatRequestDto request) {
        String message = request.message().trim();
        log.info("Chat request: userId={}, messageLength={}", userId, message.length());

        // 1. Profile (optional for chat — we still respond without it)
        StudentProfile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            log.debug("No profile found for chat userId={} — proceeding without profile context", userId);
        }

        // 2. Total remaining budget across all categories this month
        LocalDate today = LocalDate.now();
        BigDecimal totalRemaining = budgetRepository
                .findByUserIdAndMonthAndYear(userId, today.getMonthValue(), today.getYear())
                .map(this::resolveTotalRemaining)
                .orElseGet(() -> {
                    log.debug("No budget for chat userId={} {}/{} — totalRemaining=0",
                            userId, today.getMonthValue(), today.getYear());
                    return BigDecimal.ZERO;
                });

        String profileContext = buildProfileContextSummary(profile);
        log.info("Forwarding chat to AI: userId={}, totalRemaining={}", userId, totalRemaining);

        return callChatWithFallback(userId, message, totalRemaining, profileContext);
    }

    // =========================================================================
    // RECOMMENDATIONS — primary → Gemini pipeline
    // =========================================================================

    private AiRecommendationResponseDto callRecommendationsWithFallback(
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
                log.warn("Primary AI returned null recommendations — Gemini fallback: userId={}, category={}",
                        userId, category);
                return callGeminiRecommendationsFallback(request, userId, category);
            }

            log.info("Primary AI recommendations OK: userId={}, category={}, source={}, items={}",
                    userId, category, response.source(), response.recommendations().size());
            return response;

        } catch (ResourceAccessException ex) {
            log.warn("Primary AI timeout (recommendations) — Gemini fallback: userId={}, category={}: {}",
                    userId, category, ex.getMessage());
            return callGeminiRecommendationsFallback(request, userId, category);
        } catch (RestClientResponseException ex) {
            log.warn("Primary AI HTTP {} (recommendations) — Gemini fallback: userId={}, category={}",
                    ex.getStatusCode(), userId, category);
            return callGeminiRecommendationsFallback(request, userId, category);
        } catch (RestClientException ex) {
            log.warn("Primary AI failure (recommendations) — Gemini fallback: userId={}, category={}: {}",
                    userId, category, ex.getMessage());
            return callGeminiRecommendationsFallback(request, userId, category);
        } catch (Exception ex) {
            log.warn("Unexpected primary AI failure (recommendations) — Gemini fallback: userId={}, category={}: {}",
                    userId, category, ex.getMessage());
            return callGeminiRecommendationsFallback(request, userId, category);
        }
    }

    /**
     * Calls Gemini, strips Markdown code-fence wrapping, parses the JSON product array.
     * Throws {@code AiServiceUnavailableException} (503) if Gemini also fails.
     */
    private AiRecommendationResponseDto callGeminiRecommendationsFallback(
            EnrichedAiRequestDto request, UUID userId, String category) {
        log.info("Gemini recommendations fallback: userId={}, category={}", userId, category);
        try {
            String raw = callGeminiForText(buildGeminiRecommendationsPrompt(request), userId, "recommendations");

            List<AiProductDto> products = parseGeminiProductArray(raw, userId, category);

            log.info("Gemini recommendations OK: userId={}, category={}, items={}",
                    userId, category, products.size());
            return new AiRecommendationResponseDto(userId, SOURCE_GEMINI, products);

        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Gemini recommendations also failed: userId={}, category={}: {}",
                    userId, category, ex.getMessage(), ex);
            throw new AiServiceUnavailableException(
                    "AI recommendations unavailable — both the primary service and Gemini fallback failed.");
        }
    }

    // =========================================================================
    // CHAT — primary → Gemini pipeline
    // =========================================================================

    private AiChatResponseDto callChatWithFallback(
            UUID userId, String message, BigDecimal totalRemaining, String profileContext) {
        try {
            PythonChatResponse response = aiRestClient.post()
                    .uri("/api/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new PythonChatRequest(
                            message,
                            profileContext + "; total remaining budget: " + totalRemaining.toPlainString() + " RON"
                    ))
                    .retrieve()
                    .body(PythonChatResponse.class);

            if (response == null || response.reply() == null || response.reply().isBlank()) {
                log.warn("Primary AI returned empty chat reply — Gemini fallback: userId={}", userId);
                return callGeminiChatFallback(userId, message, totalRemaining, profileContext);
            }

            log.info("Primary AI chat OK: userId={}", userId);
            return new AiChatResponseDto(response.reply(), SOURCE_LLM);

        } catch (ResourceAccessException ex) {
            log.warn("Primary AI timeout (chat) — Gemini fallback: userId={}: {}", userId, ex.getMessage());
            return callGeminiChatFallback(userId, message, totalRemaining, profileContext);
        } catch (RestClientResponseException ex) {
            log.warn("Primary AI HTTP {} (chat) — Gemini fallback: userId={}", ex.getStatusCode(), userId);
            return callGeminiChatFallback(userId, message, totalRemaining, profileContext);
        } catch (RestClientException ex) {
            log.warn("Primary AI failure (chat) — Gemini fallback: userId={}: {}", userId, ex.getMessage());
            return callGeminiChatFallback(userId, message, totalRemaining, profileContext);
        } catch (Exception ex) {
            log.warn("Unexpected primary AI failure (chat) — Gemini fallback: userId={}: {}",
                    userId, ex.getMessage());
            return callGeminiChatFallback(userId, message, totalRemaining, profileContext);
        }
    }

    /**
     * Calls Gemini for a plain-text conversational reply.
     * Throws {@code AiServiceUnavailableException} (503) if Gemini also fails.
     */
    private AiChatResponseDto callGeminiChatFallback(
            UUID userId, String message, BigDecimal totalRemaining, String profileContext) {
        log.info("Gemini chat fallback: userId={}", userId);
        try {
            String reply = callGeminiForText(
                    buildGeminiChatPrompt(message, totalRemaining, profileContext),
                    userId,
                    "chat"
            ).trim();

            log.info("Gemini chat OK: userId={}", userId);
            return new AiChatResponseDto(reply, SOURCE_GEMINI);

        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Gemini chat also failed: userId={}: {}", userId, ex.getMessage(), ex);
            throw new AiServiceUnavailableException(
                    "AI chat service unavailable — both the primary service and Gemini fallback failed.");
        }
    }

    // =========================================================================
    // SHARED GEMINI HTTP CALL
    // =========================================================================

    /**
     * Sends a single prompt to Gemini and returns the raw text from the first candidate.
     * Validates structure before returning so callers can trust the result is non-null/non-blank.
     */
    private String callGeminiForText(String prompt, UUID userId, String operation) {
        GeminiRequest body = new GeminiRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt))))
        );

        GeminiResponse response = geminiRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(GEMINI_PATH)
                        .queryParam("key", geminiApiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()
                || response.candidates().get(0).content() == null
                || response.candidates().get(0).content().parts() == null
                || response.candidates().get(0).content().parts().isEmpty()) {
            log.error("Gemini returned empty/null structure: userId={}, operation={}", userId, operation);
            throw new AiServiceUnavailableException("Gemini API returned an empty response.");
        }

        String raw = response.candidates().get(0).content().parts().get(0).text();
        log.debug("Gemini raw text [{}]: userId={}, length={}", operation, userId,
                raw != null ? raw.length() : 0);
        return raw != null ? raw : "";
    }

    // =========================================================================
    // PROMPT ENGINEERING
    // =========================================================================

    private String buildGeminiRecommendationsPrompt(EnrichedAiRequestDto dto) {
        EnrichedAiRequestDto.ProfileContext ctx = dto.profileContext();
        String noteSection = dto.userNote() != null
                ? "Special student request: \"" + dto.userNote() + "\""
                : "No special request from the student.";

        return """
                Act as a student finance app. A student needs budget-aware product recommendations.

                === Student Context ===
                Category: %s
                Remaining budget this month: %s RON
                Living situation: %s
                Eating habit: %s
                Home package frequency: %s
                %s

                === Your Task ===
                Invent 3 highly realistic, student-friendly products or deals for this category.
                Prioritise affordability, practical value, and fit with the student's lifestyle.

                CRITICAL RULES — violating any rule makes your response unusable:
                1. Reply with ONLY a valid JSON array. Zero prose, zero markdown, zero code fences.
                2. The array must contain exactly 3 objects.
                3. Every object must have EXACTLY these fields:
                   {
                     "productId": "<kebab-case identifier, e.g. tofu-block-400g>",
                     "name": "<product display name>",
                     "price": <price in RON as a number>,
                     "category": "<product sub-category label>",
                     "storeName": "<where to buy it>",
                     "isPartner": false,
                     "reason": "<one sentence explaining why this fits the student>"
                   }
                """.formatted(
                dto.category(),
                dto.remainingBudget().toPlainString(),
                ctx.livingArea(),
                ctx.eatingHabit(),
                ctx.homePackageFrequency(),
                noteSection
        );
    }

    private String buildGeminiChatPrompt(
            String message, BigDecimal totalRemaining, String profileContext) {
        return """
                You are StuFi, a helpful, Gen-Z friendly financial assistant for students. \
                Keep your tone warm, concise, and empathetic.

                Student financial context:
                - Total remaining budget this month: %s RON
                - Profile: %s

                The student says: "%s"

                Reply in under 3 short paragraphs. Do NOT use markdown formatting (no bold, no bullets, no headers).
                """.formatted(
                totalRemaining.toPlainString(),
                profileContext,
                message
        );
    }

    // =========================================================================
    // GEMINI RESPONSE PARSING
    // =========================================================================

    /**
     * Strips optional Markdown code-fence wrapping Gemini adds despite prompt instructions,
     * then deserialises the JSON array into a list of {@link AiProductDto}.
     *
     * <p>Handles: bare array {@code [...]}, {@code ```json\n...\n```}, {@code ```\n...\n```}.
     */
    private List<AiProductDto> parseGeminiProductArray(String rawText, UUID userId, String category) {
        String cleaned = rawText.trim();

        if (cleaned.startsWith("```")) {
            int nl = cleaned.indexOf('\n');
            cleaned = (nl != -1) ? cleaned.substring(nl + 1) : cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
        }

        log.debug("Gemini cleaned product JSON: userId={}, category={}, json={}", userId, category, cleaned);

        try {
            return objectMapper.readValue(cleaned, new TypeReference<List<AiProductDto>>() {});
        } catch (Exception ex) {
            log.error("Failed to parse Gemini product array: userId={}, category={}, cleaned={}",
                    userId, category, cleaned, ex);
            throw new AiServiceUnavailableException(
                    "Gemini returned a product list that could not be parsed.");
        }
    }

    // =========================================================================
    // BUDGET HELPERS
    // =========================================================================

    /**
     * Remaining budget for ONE specific category: allocated − spent, clamped ≥ 0.
     * Overspent categories return 0 so the AI knows the allocation is exhausted.
     */
    private BigDecimal resolveCategoryRemaining(
            MonthlyBudget budget, String normalisedCategory, UUID userId) {
        return budget.getCategories().stream()
                .filter(c -> c.getName().equalsIgnoreCase(normalisedCategory))
                .findFirst()
                .map(c -> {
                    BigDecimal r = c.getAllocatedAmount().subtract(c.getSpentAmount()).max(BigDecimal.ZERO);
                    log.debug("Category '{}' userId={}: allocated={}, spent={}, remaining={}",
                            normalisedCategory, userId, c.getAllocatedAmount(), c.getSpentAmount(), r);
                    return r;
                })
                .orElseGet(() -> {
                    log.debug("Category '{}' not in budget for userId={}", normalisedCategory, userId);
                    return BigDecimal.ZERO;
                });
    }

    /**
     * Total remaining budget = sum of max(0, allocated − spent) across ALL categories.
     */
    private BigDecimal resolveTotalRemaining(MonthlyBudget budget) {
        return budget.getCategories().stream()
                .map(BudgetCategory::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(
                        budget.getCategories().stream()
                                .map(BudgetCategory::getSpentAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                )
                .max(BigDecimal.ZERO);
    }

    // =========================================================================
    // PROFILE HELPERS
    // =========================================================================

    private String buildProfileContextSummary(StudentProfile profile) {
        if (profile == null) {
            return "profile not available";
        }
        return "living area: " + profile.getLivingArea()
                + ", eating habit: " + profile.getEatingHabit()
                + ", home package frequency: " + profile.getHomePackageFrequency();
    }
}
