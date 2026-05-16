package org.backendcompas.modules.marketplace.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "marketplace_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "MarketplaceItem",
    description = "Persistent marketplace listing created by a student seller. Enums are stored as VARCHAR values."
)
public class MarketplaceItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    @Schema(description = "Unique marketplace item identifier.", example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34")
    private UUID id;

    @Column(nullable = false)
    @Schema(description = "UUID of the student seller that owns this listing.", example = "0fcb4ce8-9a62-4f4f-8a28-76c5c5e8d4e3")
    private UUID sellerId;

    @Column(name = "contact_phone", nullable = false, length = 20)
    @Schema(description = "Contact phone number shown to interested students.", example = "+40722123456")
    private String contactPhone;

    @Column(nullable = false, length = 100)
    @Schema(description = "Short title shown in the marketplace feed.", example = "Math 1 Course Notes")
    private String title;

    @Column(nullable = false, length = 2000)
    @Schema(description = "Detailed student-facing description of the item.", example = "Clean handwritten notes for Math 1, including seminar problems and exam recap pages.")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    @Schema(description = "Price of the item in RON.", example = "35.00")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Schema(description = "Strict category enum for the listed item.", example = "BOOKS_NOTES")
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Schema(description = "Strict condition enum for the item.", example = "GOOD")
    private ItemCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Schema(description = "Strict lifecycle status enum for the listing.", example = "ACTIVE")
    @Builder.Default
    private ItemStatus status = ItemStatus.ACTIVE;

    @Column(nullable = false)
    @Schema(description = "Whether the listing is boosted for premium visibility. Boosted listings are always returned first.", example = "true")
    @Builder.Default
    private Boolean isBoosted = Boolean.FALSE;

    /** Denormalized seller location snapshot — captured at listing creation time. */
    @Column(name = "seller_city_id")
    @Schema(description = "City UUID of the seller at listing creation time (proximity ranking).")
    private UUID sellerCityId;

    @Column(name = "seller_faculty_id")
    @Schema(description = "Faculty UUID of the seller at listing creation time (proximity ranking).")
    private UUID sellerFacultyId;

    @Column(name = "seller_dorm_id")
    @Schema(description = "Dorm UUID of the seller at listing creation time — null if seller has no dorm.")
    private UUID sellerDormId;

    @ElementCollection
    @CollectionTable(name = "student_marketplace_tags", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag", length = 100, nullable = false)
    @Schema(description = "Tags that improve search discoverability.", example = "[\"math\", \"year-1\", \"exam-prep\"]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "student_marketplace_images", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_url", length = 512, nullable = false)
    @Schema(description = "Cloudinary URLs associated with the item.", example = "[\"https://res.cloudinary.com/stufi/image/upload/v1715862200/marketplace/math-notes-cover.jpg\"]")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Schema(description = "Timestamp when the listing was created.", example = "2026-05-16T09:15:30Z")
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    @Schema(description = "Timestamp when the listing was last updated.", example = "2026-05-16T10:05:12Z")
    private Instant updatedAt;
}
