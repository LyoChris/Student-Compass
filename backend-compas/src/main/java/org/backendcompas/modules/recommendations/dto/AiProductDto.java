package org.backendcompas.modules.recommendations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        name = "AiProduct",
        description = """
                Single product recommendation produced by the internal AI recommendation service.
                The model is intentionally frontend-ready: it contains display text, commercial
                metadata, partner-store signal, and a short explanation that can be shown directly
                in recommendation cards.
                """
)
public record AiProductDto(

        @NotBlank(message = "productId is required")
        @Size(max = 120, message = "productId must be at most 120 characters")
        @Schema(
                description = "Stable product identifier from the marketplace, partner catalog, or AI fallback catalog.",
                example = "partner-kaufland-rice-1kg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String productId,

        @NotBlank(message = "name is required")
        @Size(max = 160, message = "name must be at most 160 characters")
        @Schema(
                description = "Human-readable product name displayed in the recommendation card.",
                example = "Rice 1kg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price must be greater than or equal to 0")
        @Schema(
                description = "Estimated or catalog price in RON. Partner products should use the current catalog price; fallback products may use a conservative estimate.",
                example = "9.50",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Double price,

        @NotBlank(message = "category is required")
        @Size(max = 80, message = "category must be at most 80 characters")
        @Schema(
                description = "Student-budget category used for grouping and filtering recommendations.",
                example = "Groceries",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String category,

        @NotBlank(message = "storeName is required")
        @Size(max = 120, message = "storeName must be at most 120 characters")
        @Schema(
                description = "Store, marketplace seller, or fallback source where the student can find this product.",
                example = "Kaufland",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String storeName,

        @NotNull(message = "isPartner is required")
        @Schema(
                description = "Whether the recommendation comes from a verified partner source with preferred placement or reliable catalog metadata.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Boolean isPartner,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        @Schema(
                description = "Short AI-generated explanation that connects the recommendation to the student's budget, habits, or recent spending context.",
                example = "Low-cost staple that fits your groceries budget and can cover several meals this week.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String reason
) {}
