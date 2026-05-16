package org.backendcompas.unit;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.backendcompas.modules.marketplace.repository.MarketplaceItemRepository;
import org.backendcompas.modules.marketplace.service.MarketplaceServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketplaceServiceImplTest {

    private final MarketplaceItemRepository marketplaceItemRepository = mock(MarketplaceItemRepository.class);
    private final MarketplaceServiceImpl service = new MarketplaceServiceImpl(marketplaceItemRepository);

    @Test
    void createItemHandlesNullAndBlankCollections() {
        UUID sellerId = UUID.randomUUID();
        CreateItemRequestDto request = CreateItemRequestDto.builder()
                .sellerId(sellerId)
                .title("  Title  ")
                .description("  Description  ")
                .price(BigDecimal.TEN)
                .category(ItemCategory.OTHER)
                .itemCondition(ItemCondition.NEW)
                .tags(null)
                .imageUrls(List.of("  ", "https://example.com/image.png  "))
                .build();

        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenAnswer(invocation -> {
            MarketplaceItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        var response = service.createItem(request);

        assertThat(response.getTags()).isEmpty();
        assertThat(response.getImageUrls()).containsExactly("https://example.com/image.png");
    }

    @Test
    void updateItemAppliesCategoryAndConditionBranches() {
        MarketplaceItem item = storedItem();
        when(marketplaceItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(marketplaceItemRepository.save(item)).thenReturn(item);

        UpdateItemRequestDto request = UpdateItemRequestDto.builder()
                .category(ItemCategory.ELECTRONICS)
                .itemCondition(ItemCondition.FAIR)
                .build();

        var response = service.updateItem(item.getId(), request);

        assertThat(response.getCategory()).isEqualTo(ItemCategory.ELECTRONICS);
        assertThat(response.getItemCondition()).isEqualTo(ItemCondition.FAIR);
    }

    @Test
    void changeStatusRejectsNullStatus() {
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> service.changeStatus(itemId, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Status is required");
    }

    @Test
    void searchRejectsNegativeMinPrice() {
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal minPrice = BigDecimal.valueOf(-1);

        assertThatThrownBy(() -> service.searchActiveItems(null, null, null, minPrice, null, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("minPrice must be greater than or equal to zero");
    }

    @Test
    void searchRejectsNegativeMaxPrice() {
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal maxPrice = BigDecimal.valueOf(-1);

        assertThatThrownBy(() -> service.searchActiveItems(null, null, null, null, maxPrice, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("maxPrice must be greater than or equal to zero");
    }

    @Test
    void searchRejectsMinPriceGreaterThanMaxPrice() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.searchActiveItems(null, null, null, BigDecimal.TEN, BigDecimal.ONE, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("minPrice must be less than or equal to maxPrice");
    }

        @Test
        void searchWithValidPriceRangeDelegatesToRepository() {
        MarketplaceItem item = storedItem();
        Pageable pageable = PageRequest.of(0, 10);

        when(marketplaceItemRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        var response = service.searchActiveItems(null, null, null, BigDecimal.ONE, BigDecimal.TEN, pageable);

        assertThat(response.getContent()).hasSize(1);
        verify(marketplaceItemRepository).findAll(any(Specification.class), eq(PageRequest.of(
            0,
            10,
            Sort.by(Sort.Direction.DESC, "isBoosted").and(Sort.by(Sort.Direction.DESC, "createdAt"))
        )));
        }

    @Test
    void searchWithBoostedOnlySortFallsBackToDefaultSecondarySort() {
        MarketplaceItem item = storedItem();
        Pageable pageable = PageRequest.of(1, 5, Sort.by("isBoosted"));

        when(marketplaceItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        var response = service.searchActiveItems(null, null, null, null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        verify(marketplaceItemRepository).findAll(any(Specification.class), eq(PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.DESC, "isBoosted").and(Sort.by(Sort.Direction.DESC, "createdAt"))
        )));
    }

    @Test
    void searchRejectsUnsupportedSortProperty() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unsupportedField"));

        assertThatThrownBy(() -> service.searchActiveItems(null, null, null, null, null, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported marketplace sort property: unsupportedField");
    }

    @Test
    void getItemThrowsWhenItemMissing() {
        UUID itemId = UUID.randomUUID();
        when(marketplaceItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItem(itemId))
                .isInstanceOf(org.backendcompas.core.exception.NotFoundException.class)
                .hasMessage("Marketplace item not found");
    }

    @Test
    void sanitizeRequestedSortHandlesNullSort() {
        Sort sort = ReflectionTestUtils.invokeMethod(service, "sanitizeRequestedSort", new Object[]{null});

        assertThat(sort).isNotNull();
        assertThat(sort.isUnsorted()).isTrue();
    }

    private MarketplaceItem storedItem() {
        MarketplaceItem item = new MarketplaceItem();
        item.setId(UUID.randomUUID());
        item.setSellerId(UUID.randomUUID());
        item.setTitle("Title");
        item.setDescription("Description");
        item.setPrice(BigDecimal.TEN);
        item.setCategory(ItemCategory.OTHER);
        item.setItemCondition(ItemCondition.NEW);
        item.setStatus(ItemStatus.ACTIVE);
        item.setIsBoosted(false);
        item.setTags(List.of());
        item.setImageUrls(List.of());
        return item;
    }
}
