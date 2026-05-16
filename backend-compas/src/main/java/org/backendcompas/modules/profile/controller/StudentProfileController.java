package org.backendcompas.modules.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.modules.profile.dto.StudentProfileRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileResponseDto;
import org.backendcompas.modules.profile.service.StudentProfileService;
import org.backendcompas.modules.profile.service.UpsertResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Student Profile Manager",
        description = "Endpoints for managing student financial onboarding profiles. "
                + "A profile owner may only access and modify their own profile. "
                + "An ADMIN may access and modify any profile."
)
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    // -------------------------------------------------------------------------
    // GET /{userId}
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get a student financial profile",
            description = """
                    Returns the complete financial onboarding profile for the user identified by `userId`.

                    **Access rules:**
                    - A user may only fetch **their own** profile.
                    - An **ADMIN** may fetch any profile.

                    Returns **404** if the user exists but has not yet submitted a profile.
                    Returns **403** if the caller is authenticated but is not the owner and not an ADMIN.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile found and returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StudentProfileResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No valid Bearer token provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is neither the profile owner nor an ADMIN",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No financial profile exists for this user ID yet",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
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
    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.user.id or hasRole('ADMIN')")
    public ResponseEntity<StudentProfileResponseDto> getProfile(
            @Parameter(
                    description = "UUID of the user whose profile to retrieve",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @PathVariable UUID userId) {

        return ResponseEntity.ok(studentProfileService.getProfile(userId));
    }

    // -------------------------------------------------------------------------
    // PUT /{userId}
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create or update a student financial profile (upsert)",
            description = """
                    **Upsert semantics:**
                    - If the user has **no profile yet**, a new one is created and the response status is **201 Created**.
                    - If the user **already has a profile**, all fields are fully overwritten and the response status is **200 OK**.

                    The `fixedExpenses` list is replaced wholesale on every call.
                    Send an **empty list** to remove all fixed expenses.

                    **Access rules:**
                    - A user may only upsert **their own** profile.
                    - An **ADMIN** may upsert any profile.

                    Returns **400** with a detailed message if any field fails validation.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated — an existing profile was found and its fields were overwritten",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StudentProfileResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "201",
                    description = "Profile created — no profile existed for this user, a new one was persisted",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StudentProfileResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure — one or more request fields are invalid. "
                            + "The `message` field in the error body lists all constraint violations.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No valid Bearer token provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is neither the profile owner nor an ADMIN",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "The supplied `userId` does not correspond to any registered user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
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
    @PutMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.user.id or hasRole('ADMIN')")
    public ResponseEntity<StudentProfileResponseDto> upsert(
            @Parameter(
                    description = "UUID of the user whose profile to create or update",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @PathVariable UUID userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Financial profile data. All top-level fields are required. "
                            + "`fixedExpenses` may be an empty list.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = StudentProfileRequestDto.class))
            )
            @Valid @RequestBody StudentProfileRequestDto dto) {

        UpsertResult result = studentProfileService.upsert(userId, dto);
        HttpStatus status = result.wasCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.profile());
    }
}
