package org.backendcompas.catalog.controller;

import org.backendcompas.catalog.dto.CatalogCityResponse;
import org.backendcompas.catalog.dto.CatalogFacultyResponse;
import org.backendcompas.catalog.repository.CityRepository;
import org.backendcompas.catalog.repository.FacultyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class OnboardingCatalogController {
    private final CityRepository cityRepository;
    private final FacultyRepository facultyRepository;

    public OnboardingCatalogController(CityRepository cityRepository, FacultyRepository facultyRepository) {
        this.cityRepository = cityRepository;
        this.facultyRepository = facultyRepository;
    }

    @GetMapping("/cities")
    public List<CatalogCityResponse> getCities() {
        return cityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CatalogCityResponse::from)
                .toList();
    }

    @GetMapping("/cities/{cityId}/faculties")
    public List<CatalogFacultyResponse> getFacultiesForCity(@PathVariable UUID cityId) {
        return facultyRepository.findAllByCityIdOrderByNameAsc(cityId)
                .stream()
                .map(CatalogFacultyResponse::from)
                .toList();
    }
}
