package org.backendcompas.modules.radar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.modules.radar.dto.CatalogCityResponse;
import org.backendcompas.modules.radar.dto.CatalogFacultyResponse;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Catalog", description = "Reference data for the onboarding flow — cities and faculties. All endpoints are public.")
@RestController
@RequestMapping("/api/v1/catalog")
public class OnboardingCatalogController {
    private final CityRepository cityRepository;
    private final FacultyRepository facultyRepository;

    public OnboardingCatalogController(CityRepository cityRepository, FacultyRepository facultyRepository) {
        this.cityRepository = cityRepository;
        this.facultyRepository = facultyRepository;
    }

    @Operation(
            summary = "List all cities",
            description = """
                    Returns all cities available on the platform, sorted alphabetically.
                    Use the `id` values to populate a city picker during registration
                    and to fetch the faculties for the chosen city.

                    This endpoint is **public** — no authentication required.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of cities (may be empty if no cities are seeded)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CatalogCityResponse.class))))
    })
    @GetMapping("/cities")
    public List<CatalogCityResponse> getCities() {
        return cityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CatalogCityResponse::from)
                .toList();
    }

    @Operation(
            summary = "List faculties for a city",
            description = """
                    Returns all faculties that belong to the specified city, sorted alphabetically.
                    Use the `id` values to populate a faculty picker during registration.

                    Only pass `facultyId` values from this list when calling `/api/v1/auth/register`
                    together with the matching `cityId` — the server validates that the faculty
                    belongs to the selected city.

                    This endpoint is **public** — no authentication required.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of faculties for the city (may be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CatalogFacultyResponse.class)))),
            @ApiResponse(responseCode = "404", description = "City with the given ID does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/cities/{cityId}/faculties")
    public List<CatalogFacultyResponse> getFacultiesForCity(
            @Parameter(description = "ID of the city whose faculties to retrieve",
                    example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID cityId) {
        return facultyRepository.findAllByCityIdOrderByNameAsc(cityId)
                .stream()
                .map(CatalogFacultyResponse::from)
                .toList();
    }
}
