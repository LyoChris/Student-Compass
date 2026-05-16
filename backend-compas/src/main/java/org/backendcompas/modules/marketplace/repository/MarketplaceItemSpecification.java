package org.backendcompas.modules.marketplace.repository;

import java.math.BigDecimal;
import java.util.Locale;

import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public final class MarketplaceItemSpecification {

    private MarketplaceItemSpecification() {
    }

    public static Specification<MarketplaceItem> marketplaceSearch(
        String search,
        ItemCategory category,
        ItemCondition condition,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        return Specification
            .where(hasStatus(ItemStatus.ACTIVE))
            .and(matchesSearch(search))
            .and(hasCategory(category))
            .and(hasCondition(condition))
            .and(priceGreaterThanOrEqualTo(minPrice))
            .and(priceLessThanOrEqualTo(maxPrice));
    }

    public static Specification<MarketplaceItem> hasStatus(ItemStatus status) {
        return (root, query, criteriaBuilder) -> status == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<MarketplaceItem> matchesSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
            Predicate descriptionMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
            return criteriaBuilder.or(titleMatch, descriptionMatch);
        };
    }

    public static Specification<MarketplaceItem> hasCategory(ItemCategory category) {
        return (root, query, criteriaBuilder) -> category == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<MarketplaceItem> hasCondition(ItemCondition condition) {
        return (root, query, criteriaBuilder) -> condition == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("itemCondition"), condition);
    }

    public static Specification<MarketplaceItem> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> minPrice == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<MarketplaceItem> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> maxPrice == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
