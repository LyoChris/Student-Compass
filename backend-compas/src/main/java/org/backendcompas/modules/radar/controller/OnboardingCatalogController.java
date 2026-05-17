package org.backendcompas.modules.radar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.modules.radar.dto.CatalogCityResponse;
import org.backendcompas.modules.radar.dto.CatalogDormResponse;
import org.backendcompas.modules.radar.dto.CatalogFacultyResponse;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.DormRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Catalog",
        description = """
                Public reference data used during registration and student profile setup.

                ## Onboarding flow
                ```
                1. GET /catalog/cities              → populate city picker
                2. GET /catalog/cities/{id}/faculties → populate faculty picker (after city selected)
                3. GET /catalog/dorms?cityId=       → populate optional dorm picker (profile page)
                ```

                All three endpoints are **public** — no Bearer token required.
                They are intentionally read-only; data is managed by database migrations and
                (coming soon) the admin catalog panel.
                """
)
@RestController
@RequestMapping("/api/v1/catalog")
public class OnboardingCatalogController {

    private final CityRepository cityRepository;
    private final FacultyRepository facultyRepository;
    private final DormRepository dormRepository;

    public OnboardingCatalogController(CityRepository cityRepository,
                                       FacultyRepository facultyRepository,
                                       DormRepository dormRepository) {
        this.cityRepository = cityRepository;
        this.facultyRepository = facultyRepository;
        this.dormRepository = dormRepository;
    }

    // -------------------------------------------------------------------------
    // GET /cities
    // -------------------------------------------------------------------------

    @Operation(
            summary = "List all cities",
            description = """
                    Returns every city that has been seeded into the platform, sorted alphabetically
                    by name.

                    **When to call:** At the start of the registration flow to populate the city
                    dropdown. Store the chosen city's `id` — you will need it to call
                    `/catalog/cities/{cityId}/faculties` and, later, `/catalog/dorms?cityId=`.

                    **Seeded cities (demo):**
                    - `11111111-1111-1111-1111-111111111111` — Iași
                    - `22222222-2222-2222-2222-222222222222` — Cluj-Napoca

                    This endpoint is **public** — no authentication required.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alphabetically sorted list of cities. Empty array if no cities have been seeded.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CatalogCityResponse.class)),
                            examples = @ExampleObject(
                                    name = "Demo cities",
                                    value = """
                                            [
                                              { "id": "11111111-1111-1111-1111-111111111111", "name": "Cluj-Napoca" },
                                              { "id": "22222222-2222-2222-2222-222222222222", "name": "Iasi" }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @GetMapping("/cities")
    public List<CatalogCityResponse> getCities() {
        return cityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CatalogCityResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // GET /cities/{cityId}/faculties
    // -------------------------------------------------------------------------

    @Operation(
            summary = "List faculties for a city",
            description = """
                    Returns all faculties that belong to the specified city, sorted alphabetically
                    by name.

                    **When to call:** After the student selects a city during registration, use
                    the city's `id` to load its faculties for the second dropdown.

                    **Important:** When calling `POST /api/v1/auth/register`, you must pass a
                    `facultyId` that was returned by this endpoint for the **same** `cityId` used
                    in the register request. The server validates this constraint and returns 400
                    if the faculty does not belong to the city.

                    **Seeded faculties (demo):**
                    - Iași → `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` — Facultatea de Informatica
                    - Cluj-Napoca → `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` — Facultatea de Medicina

                    This endpoint is **public** — no authentication required.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alphabetically sorted list of faculties for the city. Empty array if the city exists but has no faculties seeded.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CatalogFacultyResponse.class)),
                            examples = @ExampleObject(
                                    name = "Faculties for Iași",
                                    value = """
                                            [
                                              {
                                                "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                                "name": "Facultatea de Informatica",
                                                "cityId": "11111111-1111-1111-1111-111111111111"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No city with the given `cityId` exists in the platform",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 404, "error": "Not Found", "message": "City not found: 00000000-0000-0000-0000-000000000000" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @GetMapping("/cities/{cityId}/faculties")
    public List<CatalogFacultyResponse> getFacultiesForCity(
            @Parameter(
                    description = "UUID of the city whose faculties to retrieve. Must be an ID returned by `GET /catalog/cities`.",
                    example = "11111111-1111-1111-1111-111111111111",
                    required = true
            )
            @PathVariable UUID cityId) {
        return facultyRepository.findAllByCityIdOrderByNameAsc(cityId)
                .stream()
                .map(CatalogFacultyResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // GET /dorms
    // -------------------------------------------------------------------------

    @Operation(
            summary = "List dorms for a city",
            description = """
                    Returns all student dorms that belong to the specified city, sorted
                    alphabetically by name.

                    **When to call:** On the student profile page, after the student indicates they
                    live in a dorm (`livingArea = DORMITORY`). The chosen dorm's `id` is then sent
                    as the optional `dormId` field in `PUT /api/v1/profiles/{userId}`.

                    **Important:** `dormId` is optional for any student, not just dormitory
                    residents — a student can skip this field. However, providing it unlocks
                    proximity-based ranking in the P2P marketplace: dorm-matched listings rank
                    higher in the recommendations feed.

                    **Proximity scoring (P2P marketplace):**
                    | Seller matches buyer's… | Score |
                    |---|---|
                    | Same dorm | 3 |
                    | Same faculty | 2 |
                    | Same city | 1 |
                    | Other | 0 |

                    **Seeded dorms (demo):**

                    *Iași (`11111111-1111-1111-1111-111111111111`)*
                    - `d0000001-…` Camin T1 Titu Maiorescu
                    - `d0000002-…` Camin T2 Titu Maiorescu
                    - `d0000003-…` Camin C Tudor Vladimirescu

                    *Cluj-Napoca (`22222222-2222-2222-2222-222222222222`)*
                    - `d0000004-…` Camin Avram Iancu
                    - `d0000005-…` Camin Observator

                    This endpoint is **public** — no authentication required.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Alphabetically sorted list of dorms for the city. Empty array if no dorms have been seeded for this city.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = CatalogDormResponse.class)),
                    examples = {
                            @ExampleObject(
                                    name = "Dorms for Iași",
                                    value = """
                                            [
                                              {
                                                "id": "d0000003-0000-0000-0000-000000000000",
                                                "name": "Camin C Tudor Vladimirescu",
                                                "cityId": "11111111-1111-1111-1111-111111111111"
                                              },
                                              {
                                                "id": "d0000001-0000-0000-0000-000000000000",
                                                "name": "Camin T1 Titu Maiorescu",
                                                "cityId": "11111111-1111-1111-1111-111111111111"
                                              },
                                              {
                                                "id": "d0000002-0000-0000-0000-000000000000",
                                                "name": "Camin T2 Titu Maiorescu",
                                                "cityId": "11111111-1111-1111-1111-111111111111"
                                              }
                                            ]
                                            """
                            ),
                            @ExampleObject(
                                    name = "Dorms for Cluj-Napoca",
                                    value = """
                                            [
                                              {
                                                "id": "d0000004-0000-0000-0000-000000000000",
                                                "name": "Camin Avram Iancu",
                                                "cityId": "22222222-2222-2222-2222-222222222222"
                                              },
                                              {
                                                "id": "d0000005-0000-0000-0000-000000000000",
                                                "name": "Camin Observator",
                                                "cityId": "22222222-2222-2222-2222-222222222222"
                                              }
                                            ]
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The `cityId` query parameter is missing or not a valid UUID",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(value = """
                            { "status": 400, "error": "Bad Request", "message": "Required request parameter 'cityId' is not present" }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @GetMapping("/dorms")
    public List<CatalogDormResponse> getDormsForCity(
            @Parameter(
                    description = "UUID of the city whose dorms to retrieve. Must be a valid city `id` from `GET /catalog/cities`. Pass the same `cityId` used in the student's registration.",
                    example = "11111111-1111-1111-1111-111111111111",
                    required = true
            )
            @RequestParam UUID cityId) {
        return dormRepository.findAllByCityIdOrderByNameAsc(cityId)
                .stream()
                .map(CatalogDormResponse::from)
                .toList();
    }
}
