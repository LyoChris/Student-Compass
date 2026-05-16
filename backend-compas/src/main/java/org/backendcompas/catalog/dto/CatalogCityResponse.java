package org.backendcompas.catalog.dto;

import org.backendcompas.catalog.entity.City;

import java.util.UUID;

public record CatalogCityResponse(UUID id, String name) {
    public static CatalogCityResponse from(City city) {
        return new CatalogCityResponse(city.getId(), city.getName());
    }
}
