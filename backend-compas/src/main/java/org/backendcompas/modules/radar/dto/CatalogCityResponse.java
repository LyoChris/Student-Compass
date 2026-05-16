package org.backendcompas.modules.radar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.radar.model.City;

import java.util.UUID;

@Schema(description = "City available for student onboarding")
public record CatalogCityResponse(
        @Schema(description = "Unique city identifier", example = "11111111-1111-1111-1111-111111111111")
        UUID id,

        @Schema(description = "Display name of the city", example = "Iasi")
        String name
) {
    public static CatalogCityResponse from(City city) {
        return new CatalogCityResponse(city.getId(), city.getName());
    }
}
