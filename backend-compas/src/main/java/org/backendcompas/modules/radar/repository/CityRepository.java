package org.backendcompas.modules.radar.repository;

import org.backendcompas.modules.radar.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {
    List<City> findAllByOrderByNameAsc();
}
