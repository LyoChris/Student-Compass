package org.backendcompas.modules.marketplace.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.ItemResponseDto;
import org.backendcompas.modules.marketplace.dto.PagedMarketplaceResponse;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.springframework.data.domain.Pageable;

public interface MarketplaceService {

    ItemResponseDto createItem(CreateItemRequestDto request);

    PagedMarketplaceResponse searchActiveItems(
        String search,
        ItemCategory category,
        ItemCondition condition,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    );

    ItemResponseDto getItem(UUID id);

    ItemResponseDto updateItem(UUID id, UpdateItemRequestDto request);

    ItemResponseDto changeStatus(UUID id, ItemStatus status);

    ItemResponseDto boostItem(UUID id);
}
