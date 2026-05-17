package org.backendcompas.modules.deals.service;

import org.backendcompas.modules.deals.dto.RadarCommentRequestDto;
import org.backendcompas.modules.deals.dto.RadarCommentResponseDto;
import org.backendcompas.modules.deals.dto.RadarDealCreateRequestDto;
import org.backendcompas.modules.deals.dto.RadarDealResponseDto;
import org.backendcompas.modules.deals.dto.VoteRequestDto;

import java.util.List;
import java.util.UUID;

public interface RadarService {

    /**
     * Reports a new deal on the Radar map.
     * Throws MutedUserException (403) if caller's trust_score < 30.
     */
    RadarDealResponseDto createDeal(UUID userId, RadarDealCreateRequestDto request);

    /**
     * Returns all ACTIVE deals whose expiresAt is still in the future.
     * Deals within radiusKm of (lat, lng) are returned when coordinates are provided;
     * pass null/null to return all active deals globally.
     */
    List<RadarDealResponseDto> getActiveDeals(Double lat, Double lng, Double radiusKm);

    /**
     * Returns full detail for one deal (any status).
     * Throws DealNotFoundException (404) if not found.
     */
    RadarDealResponseDto getDeal(UUID dealId);

    /**
     * Casts a vote on a deal.
     * Karma engine: UPVOTE → poster +2 trust; DOWNVOTE → poster -2 trust (floor 0).
     * If net score drops to ≤ -5 the deal is auto-expired.
     * Throws DealNotFoundException (404), AlreadyVotedException (409).
     * Uses pessimistic locking to prevent concurrent vote race conditions.
     */
    void voteOnDeal(UUID userId, UUID dealId, VoteRequestDto request);

    /**
     * Posts a comment on a deal.
     * Throws MutedUserException (403) if caller's trust_score < 30.
     * Throws DealNotFoundException (404) if deal not found.
     */
    RadarCommentResponseDto addComment(UUID userId, UUID dealId, RadarCommentRequestDto request);
}
