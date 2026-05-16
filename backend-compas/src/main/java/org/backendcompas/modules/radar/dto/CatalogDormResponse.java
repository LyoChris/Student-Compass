package org.backendcompas.modules.radar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.radar.model.Dorm;

import java.util.UUID;

@Schema(description = "Dorm available for selection in the student profile")
public record CatalogDormResponse(

        @Schema(description = "Dorm UUID", example = "d0000001-0000-0000-0000-000000000000")
        UUID id,

        @Schema(description = "Dorm name", example = "Camin T1 Titu Maiorescu")
        String name,

        @Schema(description = "City UUID this dorm belongs to", example = "11111111-1111-1111-1111-111111111111")
        UUID cityId
) {
    public static CatalogDormResponse from(Dorm dorm) {
        return new CatalogDormResponse(dorm.getId(), dorm.getName(), dorm.getCity().getId());
    }
}
