package org.backendcompas.modules.marketplace.repository;

import java.util.UUID;

import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, UUID>, JpaSpecificationExecutor<MarketplaceItem> {

    Page<MarketplaceItem> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);
}
