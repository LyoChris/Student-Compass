package org.backendcompas.modules.radar.repository;

import org.backendcompas.modules.radar.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultyRepository extends JpaRepository<Faculty, UUID> {
    List<Faculty> findAllByCityIdOrderByNameAsc(UUID cityId);

    boolean existsByIdAndCityId(UUID id, UUID cityId);

    Optional<Faculty> findByIdAndCityId(UUID id, UUID cityId);
}
