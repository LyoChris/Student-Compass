package org.backendcompas.modules.deals.repository;

import jakarta.persistence.LockModeType;
import org.backendcompas.modules.deals.model.DealStatus;
import org.backendcompas.modules.deals.model.RadarDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RadarDealRepository extends JpaRepository<RadarDeal, UUID> {

    @Query("SELECT d FROM RadarDeal d WHERE d.status = :status AND d.expiresAt > :now")
    List<RadarDeal> findAllActiveNotExpired(
            @Param("status") DealStatus status,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM RadarDeal d WHERE d.id = :id")
    Optional<RadarDeal> findByIdWithLock(@Param("id") UUID id);
}
