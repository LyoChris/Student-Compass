package org.backendcompas.modules.deals.repository;

import org.backendcompas.modules.deals.model.RadarComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RadarCommentRepository extends JpaRepository<RadarComment, UUID> {

    List<RadarComment> findByDealIdOrderByCreatedAtAsc(UUID dealId);
}
