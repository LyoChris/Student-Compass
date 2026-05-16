package org.backendcompas.modules.deals.repository;

import org.backendcompas.modules.deals.model.RadarVote;
import org.backendcompas.modules.deals.model.RadarVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RadarVoteRepository extends JpaRepository<RadarVote, RadarVoteId> {

    @Query("SELECT COUNT(v) > 0 FROM RadarVote v WHERE v.userId = :userId AND v.deal.id = :dealId")
    boolean existsByUserIdAndDealId(@Param("userId") UUID userId, @Param("dealId") UUID dealId);

    @Query("SELECT COALESCE(SUM(CASE WHEN v.voteType = 'UPVOTE' THEN 1 ELSE -1 END), 0) FROM RadarVote v WHERE v.deal.id = :dealId")
    int netScore(@Param("dealId") UUID dealId);
}
