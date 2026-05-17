package org.backendcompas.modules.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

import org.backendcompas.core.validation.NullOrNotBlank;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
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
    name = "UpdateItemRequestDto",
    description = "Partial update payload for mutable marketplace listing fields. Omitted fields keep their current values."
)
public class UpdateItemRequestDto {

    @Size(max = 100)
    @Pattern(regexp = ".*\\S.*", message = "title must contain non-whitespace characters")
    @Schema(description = "Updated concise title. Null means unchanged.", example = "Math 1 Course Notes + Exam Recap", minLength = 1, maxLength = 100, nullable = true)
    private String title;

    @Size(max = 2000)
    @Pattern(regexp = ".*\\S.*", message = "description must contain non-whitespace characters")
    @Schema(description = "Updated detailed description. Null means unchanged.", example = "Includes seminar exercises, midterm summaries, and a two-page formula recap.", minLength = 1, maxLength = 2000, nullable = true)
    private String description;

    @Positive
    @Schema(description = "Updated price in RON. Null means unchanged.", example = "40.00", nullable = true)
    private BigDecimal price;

    @Schema(description = "Updated strict marketplace category enum. Null means unchanged.", example = "BOOKS_NOTES", allowableValues = {"BOOKS_NOTES", "ELECTRONICS", "DORM_APPLIANCES", "CLOTHING", "OTHER"}, nullable = true)
    private ItemCategory category;

    @Schema(description = "Updated strict item condition enum. Null means unchanged.", example = "LIKE_NEW", allowableValues = {"NEW", "LIKE_NEW", "GOOD", "FAIR"}, nullable = true)
    private ItemCondition itemCondition;

    @Size(max = 10)
    @Schema(description = "Replacement tags list. Null means unchanged; empty list clears tags.", example = "[\"math\", \"exam-prep\", \"study-pack\"]", nullable = true, maxLength = 10)
    private List<@NotBlank(message = "tag must contain non-whitespace characters") @Size(max = 100) String> tags;

    @Size(max = 8)
    @Schema(description = "Replacement Cloudinary image URL list. Null means unchanged; empty list clears images.", example = "[\"https://res.cloudinary.com/stufi/image/upload/v1715862100/marketplace/math-notes-page-1.jpg\"]", nullable = true, maxLength = 8)
    private List<@NotBlank(message = "imageUrl must contain non-whitespace characters") @Size(max = 512) String> imageUrls;

    @AssertTrue(message = "title must contain non-whitespace characters")
    public boolean isTitleValid() {
        return title == null || !title.trim().isEmpty();
    }

    @AssertTrue(message = "description must contain non-whitespace characters")
    public boolean isDescriptionValid() {
        return description == null || !description.trim().isEmpty();
    }
}
