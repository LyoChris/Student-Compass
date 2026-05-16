package org.backendcompas.modules.deals.service;

import org.backendcompas.core.exception.AlreadyVotedException;
import org.backendcompas.core.exception.DealNotFoundException;
import org.backendcompas.core.exception.MutedUserException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.deals.dto.RadarCommentRequestDto;
import org.backendcompas.modules.deals.dto.RadarCommentResponseDto;
import org.backendcompas.modules.deals.dto.RadarDealCreateRequestDto;
import org.backendcompas.modules.deals.dto.RadarDealResponseDto;
import org.backendcompas.modules.deals.dto.VoteRequestDto;
import org.backendcompas.modules.deals.model.DealStatus;
import org.backendcompas.modules.deals.model.RadarComment;
import org.backendcompas.modules.deals.model.RadarDeal;
import org.backendcompas.modules.deals.model.RadarVote;
import org.backendcompas.modules.deals.model.VoteType;
import org.backendcompas.modules.deals.repository.RadarCommentRepository;
import org.backendcompas.modules.deals.repository.RadarDealRepository;
import org.backendcompas.modules.deals.repository.RadarVoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RadarServiceImpl implements RadarService {

    private static final int MUTE_THRESHOLD = 30;
    private static final int KARMA_DELTA = 2;
    private static final int AUTO_EXPIRE_NET_SCORE = -5;

    private final RadarDealRepository dealRepository;
    private final RadarCommentRepository commentRepository;
    private final RadarVoteRepository voteRepository;
    private final UserRepository userRepository;

    public RadarServiceImpl(RadarDealRepository dealRepository,
                            RadarCommentRepository commentRepository,
                            RadarVoteRepository voteRepository,
                            UserRepository userRepository) {
        this.dealRepository = dealRepository;
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RadarDealResponseDto createDeal(UUID userId, RadarDealCreateRequestDto request) {
        User user = requireUser(userId);
        requireNotMuted(user);

        RadarDeal deal = new RadarDeal();
        deal.setUserId(userId);
        deal.setTitle(request.title().trim());
        deal.setDescription(request.description() != null ? request.description().trim() : null);
        deal.setCategory(request.category());
        deal.setLatitude(request.latitude());
        deal.setLongitude(request.longitude());
        deal.setExpiresAt(request.expiresAt());
        deal.setStatus(DealStatus.ACTIVE);

        return toDto(dealRepository.save(deal), user.getTrustScore());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RadarDealResponseDto> getActiveDeals(Double lat, Double lng, Double radiusKm) {
        List<RadarDeal> active = dealRepository.findAllActiveNotExpired(DealStatus.ACTIVE, LocalDateTime.now());

        if (lat != null && lng != null && radiusKm != null) {
            active = active.stream()
                    .filter(d -> haversineKm(lat, lng,
                            d.getLatitude().doubleValue(),
                            d.getLongitude().doubleValue()) <= radiusKm)
                    .toList();
        }

        return active.stream()
                .map(d -> {
                    int reporterTrustScore = userRepository.findById(d.getUserId())
                            .map(User::getTrustScore)
                            .orElse(0);
                    return toDto(d, reporterTrustScore);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RadarDealResponseDto getDeal(UUID dealId) {
        RadarDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException(dealId.toString()));
        int reporterTrustScore = userRepository.findById(deal.getUserId())
                .map(User::getTrustScore)
                .orElse(0);
        return toDto(deal, reporterTrustScore);
    }

    @Override
    public void voteOnDeal(UUID userId, UUID dealId, VoteRequestDto request) {
        if (voteRepository.existsByUserIdAndDealId(userId, dealId)) {
            throw new AlreadyVotedException();
        }

        // Pessimistic lock on the deal row to prevent concurrent vote races
        RadarDeal deal = dealRepository.findByIdWithLock(dealId)
                .orElseThrow(() -> new DealNotFoundException(dealId.toString()));

        RadarVote vote = new RadarVote();
        vote.setUserId(userId);
        vote.setDeal(deal);
        vote.setVoteType(request.voteType());
        voteRepository.save(vote);

        // Karma engine: adjust poster's trust score
        User poster = requireUser(deal.getUserId());
        if (request.voteType() == VoteType.UPVOTE) {
            poster.setTrustScore(poster.getTrustScore() + KARMA_DELTA);
        } else {
            poster.setTrustScore(Math.max(0, poster.getTrustScore() - KARMA_DELTA));
        }
        userRepository.save(poster);

        // Auto-expire if net score drops to ≤ AUTO_EXPIRE_NET_SCORE
        int net = voteRepository.netScore(dealId);
        if (net <= AUTO_EXPIRE_NET_SCORE) {
            deal.setStatus(DealStatus.EXPIRED);
            dealRepository.save(deal);
        }
    }

    @Override
    public RadarCommentResponseDto addComment(UUID userId, UUID dealId, RadarCommentRequestDto request) {
        User user = requireUser(userId);
        requireNotMuted(user);

        RadarDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException(dealId.toString()));

        RadarComment comment = new RadarComment();
        comment.setDeal(deal);
        comment.setUserId(userId);
        comment.setContent(request.content().trim());

        return toCommentDto(commentRepository.save(comment));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void requireNotMuted(User user) {
        if (user.getTrustScore() < MUTE_THRESHOLD) {
            throw new MutedUserException();
        }
    }

    private RadarDealResponseDto toDto(RadarDeal deal, int reporterTrustScore) {
        int net = voteRepository.netScore(deal.getId());
        List<RadarCommentResponseDto> comments = commentRepository
                .findByDealIdOrderByCreatedAtAsc(deal.getId())
                .stream()
                .map(this::toCommentDto)
                .toList();

        return new RadarDealResponseDto(
                deal.getId(),
                deal.getUserId(),
                reporterTrustScore,
                deal.getTitle(),
                deal.getDescription(),
                deal.getCategory(),
                deal.getLatitude(),
                deal.getLongitude(),
                deal.getExpiresAt(),
                deal.getStatus(),
                net,
                comments,
                deal.getCreatedAt()
        );
    }

    private RadarCommentResponseDto toCommentDto(RadarComment comment) {
        return new RadarCommentResponseDto(
                comment.getId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    /**
     * Haversine great-circle distance in kilometres.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
