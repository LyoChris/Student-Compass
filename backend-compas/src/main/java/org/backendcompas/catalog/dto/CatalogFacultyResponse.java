package org.backendcompas.catalog.dto;

import org.backendcompas.catalog.entity.Faculty;

import java.util.UUID;

public record CatalogFacultyResponse(UUID id, UUID cityId, String name) {
    public static CatalogFacultyResponse from(Faculty faculty) {
        return new CatalogFacultyResponse(faculty.getId(), faculty.getCity().getId(), faculty.getName());
    }
}
