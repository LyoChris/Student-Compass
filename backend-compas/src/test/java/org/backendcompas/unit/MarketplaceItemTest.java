package org.backendcompas.unit;

import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarketplaceItemTest {

    @Test
    void noArgsConstructorInitializesDefaultValues() {
        MarketplaceItem item = new MarketplaceItem();

        assertThat(item.getStatus()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(item.getIsBoosted()).isFalse();
        assertThat(item.getTags()).isEmpty();
        assertThat(item.getImageUrls()).isEmpty();
    }

    @Test
    void builderUsesDefaultValues() {
        MarketplaceItem item = MarketplaceItem.builder().build();

        assertThat(item.getStatus()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(item.getIsBoosted()).isFalse();
        assertThat(item.getTags()).isEmpty();
        assertThat(item.getImageUrls()).isEmpty();
    }

    @Test
    void allArgsConstructorAndSettersExposeState() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant updatedAt = createdAt.plusSeconds(60);

        MarketplaceItem item = new MarketplaceItem(
                id,
                sellerId,
                "Title",
                "Description",
                BigDecimal.TEN,
                ItemCategory.OTHER,
                ItemCondition.GOOD,
                ItemStatus.SOLD,
                Boolean.TRUE,
                List.of("tag"),
                List.of("https://example.com/image.png"),
                createdAt,
                updatedAt
        );

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getSellerId()).isEqualTo(sellerId);
        assertThat(item.getTitle()).isEqualTo("Title");
        assertThat(item.getDescription()).isEqualTo("Description");
        assertThat(item.getPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(item.getCategory()).isEqualTo(ItemCategory.OTHER);
        assertThat(item.getItemCondition()).isEqualTo(ItemCondition.GOOD);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.SOLD);
        assertThat(item.getIsBoosted()).isTrue();
        assertThat(item.getTags()).containsExactly("tag");
        assertThat(item.getImageUrls()).containsExactly("https://example.com/image.png");
        assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        assertThat(item.getUpdatedAt()).isEqualTo(updatedAt);

        item.setTitle("Updated");
        item.setDescription("Updated description");
        item.setPrice(BigDecimal.ONE);
        item.setCategory(ItemCategory.BOOKS_NOTES);
        item.setItemCondition(ItemCondition.NEW);
        item.setStatus(ItemStatus.ACTIVE);
        item.setIsBoosted(Boolean.FALSE);
        item.setTags(List.of("updated-tag"));
        item.setImageUrls(List.of("https://example.com/updated.png"));

        assertThat(item.getTitle()).isEqualTo("Updated");
        assertThat(item.getDescription()).isEqualTo("Updated description");
        assertThat(item.getPrice()).isEqualTo(BigDecimal.ONE);
        assertThat(item.getCategory()).isEqualTo(ItemCategory.BOOKS_NOTES);
        assertThat(item.getItemCondition()).isEqualTo(ItemCondition.NEW);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(item.getIsBoosted()).isFalse();
        assertThat(item.getTags()).containsExactly("updated-tag");
        assertThat(item.getImageUrls()).containsExactly("https://example.com/updated.png");
    }
}
