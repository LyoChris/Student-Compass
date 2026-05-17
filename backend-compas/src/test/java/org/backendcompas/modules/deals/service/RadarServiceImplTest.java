package org.backendcompas.modules.deals.service;

import org.backendcompas.core.exception.AlreadyVotedException;
import org.backendcompas.core.exception.DealNotFoundException;
import org.backendcompas.core.exception.MutedUserException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.deals.dto.RadarCommentRequestDto;
import org.backendcompas.modules.deals.dto.RadarDealCreateRequestDto;
import org.backendcompas.modules.deals.dto.RadarDealResponseDto;
import org.backendcompas.modules.deals.dto.VoteRequestDto;
import org.backendcompas.modules.deals.model.DealStatus;
import org.backendcompas.modules.deals.model.RadarComment;
import org.backendcompas.modules.deals.model.RadarDeal;
import org.backendcompas.modules.deals.model.RadarDealCategory;
import org.backendcompas.modules.deals.model.VoteType;
import org.backendcompas.modules.deals.repository.RadarCommentRepository;
import org.backendcompas.modules.deals.repository.RadarDealRepository;
import org.backendcompas.modules.deals.repository.RadarVoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RadarServiceImplTest {

    @Mock
    private RadarDealRepository dealRepository;

    @Mock
    private RadarCommentRepository commentRepository;

    @Mock
    private RadarVoteRepository voteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RadarServiceImpl radarService;

    @Test
    void createDealRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RadarDealCreateRequestDto request = buildCreateRequest();

        assertThatThrownBy(() -> radarService.createDeal(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createDealRejectsMutedUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setTrustScore(10);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        RadarDealCreateRequestDto request = buildCreateRequest();

        assertThatThrownBy(() -> radarService.createDeal(userId, request))
                .isInstanceOf(MutedUserException.class);
    }

    @Test
    void createDealStoresTrimmedFields() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setTrustScore(90);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        RadarDeal saved = new RadarDeal();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setTitle("Title");
        saved.setDescription("Desc");
        saved.setCategory(RadarDealCategory.FOOD);
        saved.setLatitude(BigDecimal.valueOf(44.0));
        saved.setLongitude(BigDecimal.valueOf(26.0));
        saved.setExpiresAt(LocalDateTime.now().plusDays(1));
        saved.setStatus(DealStatus.ACTIVE);

        when(dealRepository.save(any(RadarDeal.class))).thenReturn(saved);
        when(voteRepository.netScore(saved.getId())).thenReturn(0);
        when(commentRepository.findByDealIdOrderByCreatedAtAsc(saved.getId())).thenReturn(List.of());

        RadarDealResponseDto response = radarService.createDeal(userId, buildCreateRequest());

        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.reportedBy()).isEqualTo(userId);
    }

    @Test
    void getActiveDealsFiltersByRadius() {
        RadarDeal inside = new RadarDeal();
        inside.setId(UUID.randomUUID());
        inside.setUserId(UUID.randomUUID());
        inside.setLatitude(BigDecimal.valueOf(44.4365));
        inside.setLongitude(BigDecimal.valueOf(26.1024));
        inside.setCategory(RadarDealCategory.FOOD);
        inside.setExpiresAt(LocalDateTime.now().plusHours(1));

        RadarDeal outside = new RadarDeal();
        outside.setId(UUID.randomUUID());
        outside.setUserId(UUID.randomUUID());
        outside.setLatitude(BigDecimal.valueOf(0.0));
        outside.setLongitude(BigDecimal.valueOf(0.0));
        outside.setCategory(RadarDealCategory.FOOD);
        outside.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(dealRepository.findAllActiveNotExpired(eq(DealStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(inside, outside));
        when(voteRepository.netScore(any(UUID.class))).thenReturn(0);
        when(commentRepository.findByDealIdOrderByCreatedAtAsc(any(UUID.class))).thenReturn(List.of());
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        List<RadarDealResponseDto> results = radarService.getActiveDeals(44.4365, 26.1024, 1.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(inside.getId());
    }

    @Test
    void getActiveDealsReturnsAllWhenNoGeoFilter() {
        RadarDeal deal = new RadarDeal();
        deal.setId(UUID.randomUUID());
        deal.setUserId(UUID.randomUUID());
        deal.setLatitude(BigDecimal.valueOf(44.0));
        deal.setLongitude(BigDecimal.valueOf(26.0));
        deal.setCategory(RadarDealCategory.FOOD);
        deal.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(dealRepository.findAllActiveNotExpired(eq(DealStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(deal));
        when(voteRepository.netScore(deal.getId())).thenReturn(1);
        when(commentRepository.findByDealIdOrderByCreatedAtAsc(deal.getId())).thenReturn(List.of());
        when(userRepository.findById(deal.getUserId())).thenReturn(Optional.empty());

        List<RadarDealResponseDto> results = radarService.getActiveDeals(null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).netVotes()).isEqualTo(1);
    }

    @Test
    void getDealRejectsMissingDeal() {
        UUID dealId = UUID.randomUUID();
        when(dealRepository.findById(dealId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> radarService.getDeal(dealId))
                .isInstanceOf(DealNotFoundException.class)
                .hasMessageContaining(dealId.toString());
    }

    @Test
    void voteOnDealRejectsDuplicateVote() {
        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        VoteRequestDto request = new VoteRequestDto(VoteType.UPVOTE);
        when(voteRepository.existsByUserIdAndDealId(userId, dealId)).thenReturn(true);

        assertThatThrownBy(() -> radarService.voteOnDeal(userId, dealId, request))
                .isInstanceOf(AlreadyVotedException.class);
    }

    @Test
    void voteOnDealUpvotesAndDoesNotExpire() {
        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();

        when(voteRepository.existsByUserIdAndDealId(userId, dealId)).thenReturn(false);

        RadarDeal deal = new RadarDeal();
        deal.setId(dealId);
        deal.setUserId(UUID.randomUUID());
        when(dealRepository.findByIdWithLock(dealId)).thenReturn(Optional.of(deal));

        User poster = new User();
        poster.setId(deal.getUserId());
        poster.setTrustScore(50);
        when(userRepository.findById(deal.getUserId())).thenReturn(Optional.of(poster));

        when(voteRepository.netScore(dealId)).thenReturn(0);

        radarService.voteOnDeal(userId, dealId, new VoteRequestDto(VoteType.UPVOTE));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTrustScore()).isEqualTo(52);
    }

    @Test
    void voteOnDealDownvotesAndExpires() {
        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();

        when(voteRepository.existsByUserIdAndDealId(userId, dealId)).thenReturn(false);

        RadarDeal deal = new RadarDeal();
        deal.setId(dealId);
        deal.setUserId(UUID.randomUUID());
        when(dealRepository.findByIdWithLock(dealId)).thenReturn(Optional.of(deal));

        User poster = new User();
        poster.setId(deal.getUserId());
        poster.setTrustScore(1);
        when(userRepository.findById(deal.getUserId())).thenReturn(Optional.of(poster));

        when(voteRepository.netScore(dealId)).thenReturn(-5);

        radarService.voteOnDeal(userId, dealId, new VoteRequestDto(VoteType.DOWNVOTE));

        assertThat(poster.getTrustScore()).isZero();
        assertThat(deal.getStatus()).isEqualTo(DealStatus.EXPIRED);
        verify(dealRepository).save(deal);
    }

    @Test
    void addCommentRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        RadarCommentRequestDto request = new RadarCommentRequestDto("Hello");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> radarService.addComment(userId, dealId, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addCommentRejectsMutedUser() {
        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        RadarCommentRequestDto request = new RadarCommentRequestDto("Hello");
        User user = new User();
        user.setId(userId);
        user.setTrustScore(10);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> radarService.addComment(userId, dealId, request))
                .isInstanceOf(MutedUserException.class);
    }

    @Test
    void addCommentRejectsMissingDeal() {
        UUID userId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        RadarCommentRequestDto request = new RadarCommentRequestDto("Hello");
        User user = new User();
        user.setId(userId);
        user.setTrustScore(60);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(dealRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> radarService.addComment(userId, dealId, request))
                .isInstanceOf(DealNotFoundException.class);
    }

    @Test
    void addCommentStoresTrimmedContent() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setTrustScore(60);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        RadarDeal deal = new RadarDeal();
        deal.setId(UUID.randomUUID());
        when(dealRepository.findById(deal.getId())).thenReturn(Optional.of(deal));

        RadarComment saved = new RadarComment();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setContent("Hello");
        when(commentRepository.save(any(RadarComment.class))).thenReturn(saved);

        var response = radarService.addComment(userId, deal.getId(), new RadarCommentRequestDto(" Hello "));

        assertThat(response.content()).isEqualTo("Hello");
        ArgumentCaptor<RadarComment> captor = ArgumentCaptor.forClass(RadarComment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Hello");
    }

    private RadarDealCreateRequestDto buildCreateRequest() {
        return new RadarDealCreateRequestDto(
                " Title ",
                " Desc ",
                RadarDealCategory.FOOD,
                BigDecimal.valueOf(44.4365),
                BigDecimal.valueOf(26.1024),
                LocalDateTime.now().plusHours(1)
        );
    }
}
