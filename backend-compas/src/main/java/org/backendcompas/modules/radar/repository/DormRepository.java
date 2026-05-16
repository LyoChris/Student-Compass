package org.backendcompas.modules.radar.repository;

import org.backendcompas.modules.radar.model.Dorm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DormRepository extends JpaRepository<Dorm, UUID> {

    List<Dorm> findAllByCityIdOrderByNameAsc(UUID cityId);
}
