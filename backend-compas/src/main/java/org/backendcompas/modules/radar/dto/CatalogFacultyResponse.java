package org.backendcompas.modules.radar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.radar.model.Faculty;

import java.util.UUID;

@Schema(description = "Faculty belonging to a specific city")
public record CatalogFacultyResponse(
        @Schema(description = "Unique faculty identifier", example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        UUID id,

        @Schema(description = "ID of the city this faculty belongs to", example = "11111111-1111-1111-1111-111111111111")
        UUID cityId,

        @Schema(description = "Display name of the faculty", example = "Facultatea de Informatica")
        String name
) {
    public static CatalogFacultyResponse from(Faculty faculty) {
        return new CatalogFacultyResponse(faculty.getId(), faculty.getCity().getId(), faculty.getName());
    }
}
