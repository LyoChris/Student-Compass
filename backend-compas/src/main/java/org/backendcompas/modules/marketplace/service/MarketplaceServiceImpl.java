package org.backendcompas.modules.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.ItemResponseDto;
import org.backendcompas.modules.marketplace.dto.PagedMarketplaceResponse;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.backendcompas.modules.marketplace.repository.MarketplaceItemRepository;
import org.backendcompas.modules.marketplace.repository.MarketplaceItemSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketplaceServiceImpl implements MarketplaceService {

    private static final Sort DEFAULT_SECONDARY_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final String BOOSTED_SORT_PROPERTY = "isBoosted";
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
        "title",
        "price",
        "category",
        "itemCondition",
        "status",
        "sellerId",
        "createdAt",
        "updatedAt"
    );

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemResponseDto createItem(CreateItemRequestDto request) {
        String contactPhone = resolveContactPhone(request);
        MarketplaceItem item = MarketplaceItem.builder()
            .sellerId(request.getSellerId())
            .contactPhone(contactPhone)
            .title(request.getTitle().trim())
            .description(request.getDescription().trim())
            .price(request.getPrice())
            .category(request.getCategory())
            .itemCondition(request.getItemCondition())
            .status(ItemStatus.ACTIVE)
            .isBoosted(Boolean.FALSE)
            .tags(safeList(request.getTags()))
            .imageUrls(safeList(request.getImageUrls()))
            .build();

        return toDto(marketplaceItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedMarketplaceResponse searchActiveItems(
        String search,
        ItemCategory category,
        ItemCondition condition,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    ) {
        validatePriceRange(minPrice, maxPrice);

        Page<MarketplaceItem> page = marketplaceItemRepository.findAll(
            MarketplaceItemSpecification.marketplaceSearch(search, category, condition, minPrice, maxPrice),
            withBoostedFirstSort(pageable)
        );

        List<ItemResponseDto> content = page.getContent()
            .stream()
            .map(this::toDto)
            .toList();

        return PagedMarketplaceResponse.builder()
            .content(content)
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .isLast(page.isLast())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getItem(UUID id) {
        return toDto(findItem(id));
    }

    @Override
    public ItemResponseDto updateItem(UUID id, UpdateItemRequestDto request) {
        MarketplaceItem item = findItem(id);

        if (request.getTitle() != null) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }
        if (request.getItemCondition() != null) {
            item.setItemCondition(request.getItemCondition());
        }
        if (request.getTags() != null) {
            item.setTags(safeList(request.getTags()));
        }
        if (request.getImageUrls() != null) {
            item.setImageUrls(safeList(request.getImageUrls()));
        }

        return toDto(marketplaceItemRepository.save(item));
    }

    @Override
    public ItemResponseDto changeStatus(UUID id, ItemStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        MarketplaceItem item = findItem(id);
        item.setStatus(status);

        return toDto(marketplaceItemRepository.save(item));
    }

    @Override
    public ItemResponseDto boostItem(UUID id) {
        MarketplaceItem item = findItem(id);
        item.setIsBoosted(Boolean.TRUE);

        return toDto(marketplaceItemRepository.save(item));
    }

    private MarketplaceItem findItem(UUID id) {
        return marketplaceItemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.signum() < 0) {
            throw new BadRequestException("minPrice must be greater than or equal to zero");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new BadRequestException("maxPrice must be greater than or equal to zero");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must be less than or equal to maxPrice");
        }
    }

    private Pageable withBoostedFirstSort(Pageable pageable) {
        Sort requestedSort = sanitizeRequestedSort(pageable.getSort());
        Sort secondarySort = requestedSort.isSorted() ? requestedSort : DEFAULT_SECONDARY_SORT;
        Sort effectiveSort = Sort.by(Sort.Direction.DESC, BOOSTED_SORT_PROPERTY).and(secondarySort);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort);
    }

    private Sort sanitizeRequestedSort(Sort requestedSort) {
        if (requestedSort == null || requestedSort.isUnsorted()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : requestedSort) {
            String property = order.getProperty();
            if (!BOOSTED_SORT_PROPERTY.equals(property)) {
                if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
                    throw new BadRequestException("Unsupported marketplace sort property: " + property);
                }
                orders.add(order);
            }
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private ItemResponseDto toDto(MarketplaceItem item) {
        return ItemResponseDto.builder()
            .id(item.getId())
            .sellerId(item.getSellerId())
            .contactPhone(item.getContactPhone())
            .title(item.getTitle())
            .description(item.getDescription())
            .price(item.getPrice())
            .category(item.getCategory())
            .itemCondition(item.getItemCondition())
            .status(item.getStatus())
            .isBoosted(item.getIsBoosted())
            .tags(List.copyOf(item.getTags()))
            .imageUrls(List.copyOf(item.getImageUrls()))
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }

    private String resolveContactPhone(CreateItemRequestDto request) {
        String requestedPhone = normalizePhone(request.getContactPhone());
        if (requestedPhone != null) {
            return requestedPhone;
        }

        User seller = userRepository.findById(request.getSellerId())
            .orElseThrow(() -> new NotFoundException("Seller not found"));

        return seller.getPhoneNumber();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
