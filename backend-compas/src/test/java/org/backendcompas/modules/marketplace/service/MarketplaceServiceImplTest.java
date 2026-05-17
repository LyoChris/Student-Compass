package org.backendcompas.modules.marketplace.service;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.ForbiddenException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.backendcompas.modules.marketplace.repository.MarketplaceItemRepository;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceImplTest {

    @Mock
    private MarketplaceItemRepository marketplaceItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    @Test
    void createItemRejectsMissingSeller() {
        CreateItemRequestDto request = baseCreateRequest();
        when(userRepository.findById(request.getSellerId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketplaceService.createItem(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Seller not found");
    }

    @Test
    void createItemUsesProvidedContactPhone() {
        CreateItemRequestDto request = baseCreateRequest();
        request.setContactPhone(" +40722123456 ");

        User seller = baseSeller(request.getSellerId());
        when(userRepository.findById(request.getSellerId())).thenReturn(Optional.of(seller));
        when(studentProfileRepository.findById(request.getSellerId())).thenReturn(Optional.empty());

        MarketplaceItem saved = MarketplaceItem.builder()
                .id(UUID.randomUUID())
                .sellerId(request.getSellerId())
                .contactPhone("+40722123456")
                .title("Title")
                .description("Desc")
                .price(request.getPrice())
                .category(request.getCategory())
                .itemCondition(request.getItemCondition())
                .status(ItemStatus.ACTIVE)
                .isBoosted(Boolean.FALSE)
                .tags(List.of("tag"))
                .imageUrls(List.of("img"))
                .sellerCityId(seller.getCity().getId())
                .sellerFacultyId(seller.getFaculty().getId())
                .build();

        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenReturn(saved);

        marketplaceService.createItem(request);

        ArgumentCaptor<MarketplaceItem> captor = ArgumentCaptor.forClass(MarketplaceItem.class);
        verify(marketplaceItemRepository).save(captor.capture());
        assertThat(captor.getValue().getContactPhone()).isEqualTo("+40722123456");
        assertThat(captor.getValue().getTags()).containsExactly("tag");
        assertThat(captor.getValue().getImageUrls()).containsExactly("img");
    }

    @Test
    void createItemUsesSellerPhoneWhenContactMissing() {
        CreateItemRequestDto request = baseCreateRequest();
        request.setContactPhone(" ");

        User seller = baseSeller(request.getSellerId());
        seller.setPhoneNumber("+40999999999");
        when(userRepository.findById(request.getSellerId())).thenReturn(Optional.of(seller));
        when(studentProfileRepository.findById(request.getSellerId())).thenReturn(Optional.of(new StudentProfile()));

        MarketplaceItem saved = MarketplaceItem.builder()
                .id(UUID.randomUUID())
                .sellerId(request.getSellerId())
                .contactPhone("+40999999999")
                .title("Title")
                .description("Desc")
                .price(request.getPrice())
                .category(request.getCategory())
                .itemCondition(request.getItemCondition())
                .status(ItemStatus.ACTIVE)
                .isBoosted(Boolean.FALSE)
                .tags(List.of())
                .imageUrls(List.of())
                .sellerCityId(seller.getCity().getId())
                .sellerFacultyId(seller.getFaculty().getId())
                .build();

        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenReturn(saved);

        marketplaceService.createItem(request);

        ArgumentCaptor<MarketplaceItem> captor = ArgumentCaptor.forClass(MarketplaceItem.class);
        verify(marketplaceItemRepository).save(captor.capture());
        assertThat(captor.getValue().getContactPhone()).isEqualTo("+40999999999");
    }

    @Test
    void searchActiveItemsValidatesPriceRange() {
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal negativePrice = BigDecimal.valueOf(-1);
        BigDecimal minPrice = BigDecimal.valueOf(10);
        BigDecimal maxPrice = BigDecimal.valueOf(1);

        assertThatThrownBy(() -> marketplaceService.searchActiveItems(null, null, null,
                negativePrice, null, pageable))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> marketplaceService.searchActiveItems(null, null, null,
                null, negativePrice, pageable))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> marketplaceService.searchActiveItems(null, null, null,
                minPrice, maxPrice, pageable))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void searchActiveItemsRejectsUnsupportedSortProperty() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalid"));

        assertThatThrownBy(() -> marketplaceService.searchActiveItems(null, null, null,
                null, null, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported marketplace sort property");
    }

    @Test
    void searchActiveItemsAddsBoostedSort() {
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
        Page<MarketplaceItem> page = new PageImpl<>(List.of());

        when(marketplaceItemRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).thenReturn(page);

        marketplaceService.searchActiveItems(null, null, null, null, null, pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(marketplaceItemRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("isBoosted")).isNotNull();
    }

    @Test
    void searchActiveItemsIgnoresBoostedInRequestedSort() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("isBoosted"));
        Page<MarketplaceItem> page = new PageImpl<>(List.of());

        when(marketplaceItemRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).thenReturn(page);

        marketplaceService.searchActiveItems(null, null, null, null, null, pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(marketplaceItemRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("isBoosted")).isNotNull();
    }

    @Test
    void getItemRejectsMissingItem() {
        UUID id = UUID.randomUUID();
        when(marketplaceItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketplaceService.getItem(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Marketplace item not found");
    }

    @Test
    void updateItemRejectsNonOwner() {
        UUID id = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        MarketplaceItem item = MarketplaceItem.builder()
                .id(id)
                .sellerId(UUID.randomUUID())
                .build();
        when(marketplaceItemRepository.findById(id)).thenReturn(Optional.of(item));

        UpdateItemRequestDto request = new UpdateItemRequestDto();

        assertThatThrownBy(() -> marketplaceService.updateItem(id, request, callerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not the owner");
    }

    @Test
    void updateItemAppliesFieldsAndSaves() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        MarketplaceItem item = MarketplaceItem.builder()
                .id(id)
                .sellerId(ownerId)
                .title("Old")
                .description("Old")
                .price(BigDecimal.TEN)
                .category(ItemCategory.BOOKS_NOTES)
                .itemCondition(ItemCondition.GOOD)
                .tags(List.of("old"))
                .imageUrls(List.of("old"))
                .build();

        when(marketplaceItemRepository.findById(id)).thenReturn(Optional.of(item));
        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenReturn(item);

        UpdateItemRequestDto request = new UpdateItemRequestDto();
        request.setTitle(" New ");
        request.setDescription(" Desc ");
        request.setPrice(BigDecimal.ONE);
        request.setCategory(ItemCategory.ELECTRONICS);
        request.setItemCondition(ItemCondition.LIKE_NEW);
        request.setTags(List.of(" tag ", " "));
        request.setImageUrls(List.of(" img "));

        marketplaceService.updateItem(id, request, ownerId);

        ArgumentCaptor<MarketplaceItem> captor = ArgumentCaptor.forClass(MarketplaceItem.class);
        verify(marketplaceItemRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("New");
        assertThat(captor.getValue().getDescription()).isEqualTo("Desc");
        assertThat(captor.getValue().getTags()).containsExactly("tag");
        assertThat(captor.getValue().getImageUrls()).containsExactly("img");
    }

    @Test
    void changeStatusRequiresStatus() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        assertThatThrownBy(() -> marketplaceService.changeStatus(id, null, ownerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Status is required");
    }

    @Test
    void changeStatusUpdatesStatus() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        MarketplaceItem item = MarketplaceItem.builder()
                .id(id)
                .sellerId(ownerId)
                .status(ItemStatus.ACTIVE)
                .build();

        when(marketplaceItemRepository.findById(id)).thenReturn(Optional.of(item));
        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenReturn(item);

        marketplaceService.changeStatus(id, ItemStatus.SOLD, ownerId);

        assertThat(item.getStatus()).isEqualTo(ItemStatus.SOLD);
    }

    @Test
    void boostItemSetsBoostedFlag() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        MarketplaceItem item = MarketplaceItem.builder()
                .id(id)
                .sellerId(ownerId)
                .isBoosted(Boolean.FALSE)
                .build();

        when(marketplaceItemRepository.findById(id)).thenReturn(Optional.of(item));
        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenReturn(item);

        marketplaceService.boostItem(id, ownerId);

        assertThat(item.getIsBoosted()).isTrue();
    }

    @Test
    void deleteItemRemovesListing() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        MarketplaceItem item = MarketplaceItem.builder()
                .id(id)
                .sellerId(ownerId)
                .build();

        when(marketplaceItemRepository.findById(id)).thenReturn(Optional.of(item));

        marketplaceService.deleteItem(id, ownerId);

        verify(marketplaceItemRepository).delete(item);
    }

    @Test
    void getMyItemsReturnsPagedResponse() {
        UUID sellerId = UUID.randomUUID();
        Page<MarketplaceItem> page = new PageImpl<>(List.of());
        when(marketplaceItemRepository.findBySellerIdOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(page);

        var response = marketplaceService.getMyItems(sellerId, PageRequest.of(0, 10));

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPageNumber()).isZero();
    }

    private CreateItemRequestDto baseCreateRequest() {
        CreateItemRequestDto request = new CreateItemRequestDto();
        request.setSellerId(UUID.randomUUID());
        request.setTitle("Title");
        request.setDescription("Desc");
        request.setPrice(BigDecimal.TEN);
        request.setCategory(ItemCategory.BOOKS_NOTES);
        request.setItemCondition(ItemCondition.GOOD);
        request.setTags(List.of(" tag "));
        request.setImageUrls(List.of(" img "));
        return request;
    }

    private User baseSeller(UUID id) {
        User seller = new User();
        seller.setId(id);
        seller.setPhoneNumber("+40722123456");
        var city = new org.backendcompas.modules.radar.model.City();
        city.setId(UUID.randomUUID());
        var faculty = new org.backendcompas.modules.radar.model.Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);
        seller.setCity(city);
        seller.setFaculty(faculty);
        return seller;
    }
}
