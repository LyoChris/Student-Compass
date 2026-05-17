package org.backendcompas.modules.marketplace.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ItemResponseDto", description = "Marketplace listing response returned to mobile and web clients.")
public class ItemResponseDto {

    @Schema(description = "Unique marketplace item identifier.", example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Schema(description = "UUID of the student seller that owns this listing.", example = "0fcb4ce8-9a62-4f4f-8a28-76c5c5e8d4e3", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID sellerId;

    @Schema(description = "Contact phone number shown to interested students.", example = "+40722123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactPhone;

    @Schema(description = "Title shown in the marketplace feed.", example = "Graphing Calculator TI-84 Plus", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Detailed student-facing description of the listing.", example = "Lightly used calculator, perfect for engineering exams. Includes fresh batteries and campus pickup near the library.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "Price in RON.", example = "180.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "Strict marketplace category enum.", example = "ELECTRONICS", allowableValues = {"BOOKS_NOTES", "ELECTRONICS", "DORM_APPLIANCES", "CLOTHING", "OTHER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private ItemCategory category;

    @Schema(description = "Strict item condition enum.", example = "LIKE_NEW", allowableValues = {"NEW", "LIKE_NEW", "GOOD", "FAIR"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private ItemCondition itemCondition;

    @Schema(description = "Current strict lifecycle status enum.", example = "ACTIVE", allowableValues = {"ACTIVE", "RESERVED", "SOLD", "INACTIVE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private ItemStatus status;

    @Schema(description = "Whether the listing is boosted for premium visibility. Boosted listings are always returned first.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isBoosted;

    @Schema(description = "City UUID of the seller (proximity ranking).", nullable = true)
    private UUID sellerCityId;

    @Schema(description = "Faculty UUID of the seller (proximity ranking).", nullable = true)
    private UUID sellerFacultyId;

    @Schema(description = "Dorm UUID of the seller — null if no dorm.", nullable = true)
    private UUID sellerDormId;

    @Schema(description = "Tags attached to the listing.", example = "[\"engineering\", \"year-2\", \"exam-ready\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> tags;

    @Schema(description = "Cloudinary image URLs attached to the listing.", example = "[\"https://res.cloudinary.com/stufi/image/upload/v1715862200/marketplace/ti84-front.jpg\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> imageUrls;

    @Schema(description = "Timestamp when the listing was created.", example = "2026-05-16T09:15:30Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;

    @Schema(description = "Timestamp when the listing was last updated.", example = "2026-05-16T10:05:12Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant updatedAt;

    @Schema(description = "Trust score of the seller (0–100+). Indicates community-verified reliability. Scores below 30 indicate a muted/low-trust seller.", example = "87", nullable = true)
    private Integer sellerTrustScore;
}
