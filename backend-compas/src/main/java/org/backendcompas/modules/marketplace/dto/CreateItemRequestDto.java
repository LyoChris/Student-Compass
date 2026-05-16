package org.backendcompas.modules.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@Schema(
    name = "CreateItemRequestDto",
    description = "Request payload used by a student to create a new marketplace listing. The item starts as ACTIVE and not boosted."
)
public class CreateItemRequestDto {

    @NotNull
    @Schema(description = "UUID of the authenticated student seller creating the listing.", example = "0fcb4ce8-9a62-4f4f-8a28-76c5c5e8d4e3", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID sellerId;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Concise title visible in the marketplace feed.", example = "Math 1 Course Notes", minLength = 1, maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Detailed item description with useful pickup or course context.", example = "Complete Math 1 lecture notes with solved seminar exercises, clean scans, and final exam summary pages.", minLength = 1, maxLength = 2000, requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotNull
    @Positive
    @Schema(description = "Listing price in RON. Must be greater than zero.", example = "35.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull
    @Schema(description = "Strict marketplace category enum.", example = "BOOKS_NOTES", allowableValues = {"BOOKS_NOTES", "ELECTRONICS", "DORM_APPLIANCES", "CLOTHING", "OTHER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private ItemCategory category;

    @NotNull
    @Schema(description = "Strict item condition enum.", example = "GOOD", allowableValues = {"NEW", "LIKE_NEW", "GOOD", "FAIR"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private ItemCondition itemCondition;

    @Size(max = 10)
    @Schema(description = "Optional tags used for discoverability. Keep tags short and student-friendly.", example = "[\"math\", \"year-1\", \"exam-prep\"]", maxLength = 10)
    private List<@NotBlank @Size(max = 100) String> tags;

    @Size(max = 8)
    @Schema(description = "Optional image URLs hosted on Cloudinary.", example = "[\"https://res.cloudinary.com/stufi/image/upload/v1715862000/marketplace/math-notes-cover.jpg\"]", maxLength = 8)
    private List<@NotBlank @Size(max = 512) String> imageUrls;
}
