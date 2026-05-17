package org.backendcompas.modules.recommendations.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Catalog category accepted by the AI microservice.")
public enum RecommendationCategory {
    FOOD,
    FURNITURE,
    ELECTRONICS
}
