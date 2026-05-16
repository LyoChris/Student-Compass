package org.backendcompas.unit;

import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.backendcompas.modules.marketplace.repository.MarketplaceItemSpecification;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketplaceItemSpecificationTest {

    @Test
    void hasStatusWithNullReturnsConjunction() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate predicate = MarketplaceItemSpecification.hasStatus(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(conjunction);
    }

    @Test
    void hasStatusWithValueReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path statusPath = mock(jakarta.persistence.criteria.Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(criteriaBuilder.equal(statusPath, ItemStatus.ACTIVE)).thenReturn(predicate);

        Predicate result = MarketplaceItemSpecification.hasStatus(ItemStatus.ACTIVE)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(predicate);
    }

    @ParameterizedTest
    @MethodSource("blankSearchValues")
    void matchesSearchWithNullOrBlankReturnsConjunction(String search) {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate predicate = MarketplaceItemSpecification.matchesSearch(search)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(conjunction);
    }

    @Test
    void matchesSearchWithValueBuildsLikePredicates() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path titlePath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path descriptionPath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Expression lowerTitle = mock(jakarta.persistence.criteria.Expression.class);
        jakarta.persistence.criteria.Expression lowerDescription = mock(jakarta.persistence.criteria.Expression.class);
        Predicate titleMatch = mock(Predicate.class);
        Predicate descriptionMatch = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);

        when(root.get("title")).thenReturn(titlePath);
        when(root.get("description")).thenReturn(descriptionPath);
        when(criteriaBuilder.lower(titlePath)).thenReturn(lowerTitle);
        when(criteriaBuilder.lower(descriptionPath)).thenReturn(lowerDescription);
        when(criteriaBuilder.like(any(jakarta.persistence.criteria.Expression.class), anyString())).thenReturn(titleMatch, descriptionMatch);
        when(criteriaBuilder.or(titleMatch, descriptionMatch)).thenReturn(orPredicate);

        Predicate result = MarketplaceItemSpecification.matchesSearch(" math ")
            .toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder, times(2)).like(any(jakarta.persistence.criteria.Expression.class), anyString());
        assertThat(result).isSameAs(orPredicate);
    }

    private static Stream<String> blankSearchValues() {
        return Stream.of(null, "   ");
    }

    @Test
    void hasCategoryWithNullReturnsConjunction() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate predicate = MarketplaceItemSpecification.hasCategory(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(conjunction);
    }

    @Test
    void hasCategoryWithValueReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path categoryPath = mock(jakarta.persistence.criteria.Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("category")).thenReturn(categoryPath);
        when(criteriaBuilder.equal(categoryPath, ItemCategory.BOOKS_NOTES)).thenReturn(predicate);

        Predicate result = MarketplaceItemSpecification.hasCategory(ItemCategory.BOOKS_NOTES)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void hasConditionWithNullReturnsConjunction() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate predicate = MarketplaceItemSpecification.hasCondition(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(conjunction);
    }

    @Test
    void hasConditionWithValueReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path conditionPath = mock(jakarta.persistence.criteria.Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("itemCondition")).thenReturn(conditionPath);
        when(criteriaBuilder.equal(conditionPath, ItemCondition.NEW)).thenReturn(predicate);

        Predicate result = MarketplaceItemSpecification.hasCondition(ItemCondition.NEW)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void priceGreaterThanOrEqualToWithNullReturnsConjunction() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate predicate = MarketplaceItemSpecification.priceGreaterThanOrEqualTo(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(conjunction);
    }

    @Test
    void priceGreaterThanOrEqualToWithValueReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path pricePath = mock(jakarta.persistence.criteria.Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("price")).thenReturn(pricePath);
        when(criteriaBuilder.greaterThanOrEqualTo(pricePath, BigDecimal.TEN)).thenReturn(predicate);

        Predicate result = MarketplaceItemSpecification.priceGreaterThanOrEqualTo(BigDecimal.TEN)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void priceLessThanOrEqualToWithNullReturnsConjunction() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate predicate = MarketplaceItemSpecification.priceLessThanOrEqualTo(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(conjunction);
    }

    @Test
    void priceLessThanOrEqualToWithValueReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path pricePath = mock(jakarta.persistence.criteria.Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("price")).thenReturn(pricePath);
        when(criteriaBuilder.lessThanOrEqualTo(pricePath, BigDecimal.valueOf(100))).thenReturn(predicate);

        Predicate result = MarketplaceItemSpecification.priceLessThanOrEqualTo(BigDecimal.valueOf(100))
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void marketplaceSearchCompositeReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path statusPath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path titlePath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path descriptionPath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path categoryPath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path conditionPath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pricePath = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Expression lowerTitle = mock(jakarta.persistence.criteria.Expression.class);
        jakarta.persistence.criteria.Expression lowerDescription = mock(jakarta.persistence.criteria.Expression.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate titleMatch = mock(Predicate.class);
        Predicate descriptionMatch = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);
        Predicate categoryPredicate = mock(Predicate.class);
        Predicate conditionPredicate = mock(Predicate.class);
        Predicate minPricePredicate = mock(Predicate.class);
        Predicate maxPricePredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(root.get("title")).thenReturn(titlePath);
        when(root.get("description")).thenReturn(descriptionPath);
        when(root.get("category")).thenReturn(categoryPath);
        when(root.get("itemCondition")).thenReturn(conditionPath);
        when(root.get("price")).thenReturn(pricePath);
        when(criteriaBuilder.equal(statusPath, ItemStatus.ACTIVE)).thenReturn(statusPredicate);
        when(criteriaBuilder.lower(titlePath)).thenReturn(lowerTitle);
        when(criteriaBuilder.lower(descriptionPath)).thenReturn(lowerDescription);
        when(criteriaBuilder.like(any(jakarta.persistence.criteria.Expression.class), anyString())).thenReturn(titleMatch, descriptionMatch);
        when(criteriaBuilder.or(titleMatch, descriptionMatch)).thenReturn(orPredicate);
        when(criteriaBuilder.equal(categoryPath, ItemCategory.ELECTRONICS)).thenReturn(categoryPredicate);
        when(criteriaBuilder.equal(conditionPath, ItemCondition.GOOD)).thenReturn(conditionPredicate);
        when(criteriaBuilder.greaterThanOrEqualTo(pricePath, BigDecimal.ONE)).thenReturn(minPricePredicate);
        when(criteriaBuilder.lessThanOrEqualTo(pricePath, BigDecimal.valueOf(100))).thenReturn(maxPricePredicate);
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(combined);

        Predicate predicate = MarketplaceItemSpecification.marketplaceSearch(
                "test", ItemCategory.ELECTRONICS, ItemCondition.GOOD,
                BigDecimal.ONE, BigDecimal.valueOf(100)
        ).toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(combined);
    }

    @Test
    void marketplaceSearchAllNullsReturnsNonNull() {
        Root<MarketplaceItem> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Path statusPath = mock(jakarta.persistence.criteria.Path.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate conjunction = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(criteriaBuilder.equal(statusPath, ItemStatus.ACTIVE)).thenReturn(statusPredicate);
        when(criteriaBuilder.conjunction()).thenReturn(conjunction);
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(combined);

        Predicate predicate = MarketplaceItemSpecification.marketplaceSearch(null, null, null, null, null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(combined);
    }
}
