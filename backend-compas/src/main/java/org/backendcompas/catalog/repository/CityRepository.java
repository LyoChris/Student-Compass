package org.backendcompas.catalog.repository;

import org.backendcompas.catalog.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {
    List<City> findAllByOrderByNameAsc();
}
