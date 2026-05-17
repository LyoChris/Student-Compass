package org.backendcompas.modules.deals.controller;

import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.deals.dto.RadarCommentRequestDto;
import org.backendcompas.modules.deals.dto.RadarCommentResponseDto;
import org.backendcompas.modules.deals.dto.RadarDealCreateRequestDto;
import org.backendcompas.modules.deals.dto.RadarDealResponseDto;
import org.backendcompas.modules.deals.dto.VoteRequestDto;
import org.backendcompas.modules.deals.model.DealStatus;
import org.backendcompas.modules.deals.model.RadarDealCategory;
import org.backendcompas.modules.deals.model.VoteType;
import org.backendcompas.modules.deals.service.RadarService;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RadarControllerTest {

    @Test
    void delegatesCreateDeal() {
        RadarService radarService = Mockito.mock(RadarService.class);
        RadarController controller = new RadarController(radarService);

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));
        RadarDealCreateRequestDto request = new RadarDealCreateRequestDto(
                "Deal",
                "Desc",
                RadarDealCategory.FOOD,
                BigDecimal.valueOf(44.0),
                BigDecimal.valueOf(26.0),
                LocalDateTime.now().plusHours(1)
        );

        RadarDealResponseDto responseDto = new RadarDealResponseDto(
                UUID.randomUUID(), userId, 50, "Deal", "Desc", RadarDealCategory.FOOD,
                BigDecimal.valueOf(44.0), BigDecimal.valueOf(26.0), LocalDateTime.now().plusHours(1),
                DealStatus.ACTIVE, 0, List.of(), LocalDateTime.now()
        );

        when(radarService.createDeal(userId, request)).thenReturn(responseDto);

        ResponseEntity<RadarDealResponseDto> response = controller.createDeal(userDetails, request);

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void delegatesGetActiveDeals() {
        RadarService radarService = Mockito.mock(RadarService.class);
        RadarController controller = new RadarController(radarService);

        when(radarService.getActiveDeals(null, null, null)).thenReturn(List.of());

        ResponseEntity<List<RadarDealResponseDto>> response = controller.getActiveDeals(null, null, null);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void delegatesGetDeal() {
        RadarService radarService = Mockito.mock(RadarService.class);
        RadarController controller = new RadarController(radarService);

        UUID dealId = UUID.randomUUID();
        RadarDealResponseDto responseDto = new RadarDealResponseDto(
                dealId, UUID.randomUUID(), 50, "Deal", null, RadarDealCategory.FOOD,
                BigDecimal.valueOf(44.0), BigDecimal.valueOf(26.0), LocalDateTime.now().plusHours(1),
                DealStatus.ACTIVE, 0, List.of(), LocalDateTime.now()
        );

        when(radarService.getDeal(dealId)).thenReturn(responseDto);

        ResponseEntity<RadarDealResponseDto> response = controller.getDeal(dealId);

        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void delegatesVoteOnDeal() {
        RadarService radarService = Mockito.mock(RadarService.class);
        RadarController controller = new RadarController(radarService);

        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));
        VoteRequestDto request = new VoteRequestDto(VoteType.UPVOTE);

        ResponseEntity<Void> response = controller.voteOnDeal(userDetails, dealId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(radarService).voteOnDeal(userId, dealId, request);
    }

    @Test
    void delegatesAddComment() {
        RadarService radarService = Mockito.mock(RadarService.class);
        RadarController controller = new RadarController(radarService);

        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(userId));
        RadarCommentRequestDto request = new RadarCommentRequestDto("Hello");

        RadarCommentResponseDto responseDto = new RadarCommentResponseDto(
                UUID.randomUUID(), userId, "Hello", LocalDateTime.now()
        );

        when(radarService.addComment(userId, dealId, request)).thenReturn(responseDto);

        ResponseEntity<RadarCommentResponseDto> response = controller.addComment(userDetails, dealId, request);

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
