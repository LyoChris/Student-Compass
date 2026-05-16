package org.backendcompas.modules.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.account.dto.UserProfileResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "Operations on the currently authenticated user's account")
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    @Operation(
            summary = "Get the current user's profile",
            description = """
                    Returns the full profile of the authenticated user identified by the **Bearer JWT**
                    in the `Authorization` header.

                    This endpoint is accessible to both **USER** and **ADMIN** roles.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Access token is missing, malformed, expired, or blacklisted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return UserProfileResponse.from(currentUser.getUser());
    }
}
