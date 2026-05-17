package org.backendcompas.modules.marketplace.controller;

import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.ItemResponseDto;
import org.backendcompas.modules.marketplace.dto.PagedMarketplaceResponse;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.service.MarketplaceService;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MarketplaceControllerTest {

    @Test
    void createItemDelegatesToService() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        CreateItemRequestDto request = new CreateItemRequestDto();
        request.setSellerId(UUID.randomUUID());
        request.setTitle("Title");
        request.setDescription("Desc");
        request.setPrice(BigDecimal.TEN);
        request.setCategory(ItemCategory.BOOKS_NOTES);
        request.setItemCondition(ItemCondition.GOOD);

        ItemResponseDto responseDto = ItemResponseDto.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .build();

        when(service.createItem(request)).thenReturn(responseDto);

        ResponseEntity<ItemResponseDto> response = controller.createItem(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void searchActiveItemsDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        PagedMarketplaceResponse responseDto = PagedMarketplaceResponse.builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .isLast(true)
                .build();

        when(service.searchActiveItems(null, null, null, null, null, PageRequest.of(0, 20))).thenReturn(responseDto);

        ResponseEntity<PagedMarketplaceResponse> response = controller.searchActiveItems(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void getItemDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        UUID id = UUID.randomUUID();
        ItemResponseDto responseDto = ItemResponseDto.builder().id(id).build();
        when(service.getItem(id)).thenReturn(responseDto);

        ResponseEntity<ItemResponseDto> response = controller.getItem(id);

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void getMyItemsDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));

        PagedMarketplaceResponse responseDto = PagedMarketplaceResponse.builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .isLast(true)
                .build();

        when(service.getMyItems(userId, PageRequest.of(0, 20))).thenReturn(responseDto);

        ResponseEntity<PagedMarketplaceResponse> response = controller.getMyItems(userDetails, PageRequest.of(0, 20));

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void updateItemDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));
        UpdateItemRequestDto request = new UpdateItemRequestDto();

        ItemResponseDto responseDto = ItemResponseDto.builder().id(id).build();
        when(service.updateItem(id, request, userId)).thenReturn(responseDto);

        ResponseEntity<ItemResponseDto> response = controller.updateItem(id, request, userDetails);

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void changeStatusDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));
        ItemResponseDto responseDto = ItemResponseDto.builder().id(id).status(ItemStatus.SOLD).build();

        when(service.changeStatus(id, ItemStatus.SOLD, userId)).thenReturn(responseDto);

        ResponseEntity<ItemResponseDto> response = controller.changeStatus(id, ItemStatus.SOLD, userDetails);

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void deleteItemDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));

        ResponseEntity<Void> response = controller.deleteItem(id, userDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void boostItemDelegates() {
        MarketplaceService service = Mockito.mock(MarketplaceService.class);
        MarketplaceController controller = new MarketplaceController(service);

        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));
        ItemResponseDto responseDto = ItemResponseDto.builder().id(id).build();

        when(service.boostItem(id, userId)).thenReturn(responseDto);

        ResponseEntity<ItemResponseDto> response = controller.boostItem(id, userDetails);

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    private User buildUser(UUID userId) {
        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);
        return user;
    }
}
