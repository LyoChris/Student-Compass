package org.backendcompas.modules.recommendations.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Budget spending category for AI product recommendations.")
public enum RecommendationCategory {
    FOOD,
    TRANSPORT,
    ENTERTAINMENT,
    HEALTH,
    CLOTHING,
    EDUCATION,
    UTILITIES,
    PERSONAL_CARE,
    SAVINGS,
    OTHER
}
