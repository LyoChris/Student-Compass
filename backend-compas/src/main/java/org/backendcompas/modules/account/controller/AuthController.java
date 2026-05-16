package org.backendcompas.modules.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.dto.AuthResponse;
import org.backendcompas.modules.account.dto.LoginRequest;
import org.backendcompas.modules.account.dto.RefreshResponse;
import org.backendcompas.modules.account.dto.RegisterRequest;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.service.AuthService;
import org.backendcompas.modules.account.service.RefreshTokenService;
import org.backendcompas.modules.account.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Authentication", description = "Register, log in, refresh tokens, and log out")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;

    @Value("${app.auth.secure-cookies:false}")
    private boolean secureCookies;

    @Value("${app.auth.refresh-cookie-path:/api/v1/auth}")
    private String refreshCookiePath;

    @Value("${app.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          TokenBlacklistService tokenBlacklistService,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(
            summary = "Register a new student account",
            description = """
                    Creates a new **USER** account. All registrations are students — ADMIN accounts
                    can only be created directly in the database.

                    On success the response body contains the **access token** and the user profile.
                    The **refresh token** is set as an HttpOnly cookie (`refresh_token`) and is **not**
                    present in the response body.

                    Business rules enforced:
                    - Email must be unique.
                    - `facultyId` must belong to the city identified by `cityId`.
                    - `password` and `confirmPassword` must match.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created — access token in body, refresh token in cookie",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation (e.g. faculty not in city, passwords do not match)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email address is already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return responseWithRefreshCookie(authService.register(request));
    }

    @Operation(
            summary = "Log in with email and password",
            description = """
                    Authenticates an existing account and returns a new **access token** (in the body)
                    together with a new **refresh token** (HttpOnly cookie).

                    Failed login attempts are counted. After **5 consecutive failures** the account
                    is locked for 15 minutes.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — access token in body, refresh token in cookie",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (missing or malformed fields)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Wrong email or password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Account is locked due to too many failed login attempts",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return responseWithRefreshCookie(authService.login(request));
    }

    @Operation(
            summary = "Refresh the access token",
            description = """
                    Issues a new **access token** using the `refresh_token` HttpOnly cookie.
                    The refresh token is **rotated** on every call — the old one is invalidated and
                    a new one is set via `Set-Cookie`.

                    **CSRF protection is required:** send the value of the `XSRF-TOKEN` cookie
                    as the `X-XSRF-TOKEN` request header.
                    """,
            security = @SecurityRequirement(name = ""),
            parameters = @Parameter(
                    in = ParameterIn.COOKIE,
                    name = "refresh_token",
                    required = true,
                    description = "HttpOnly refresh token cookie set during login or registration",
                    schema = @Schema(type = "string")
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued — new refresh token set in cookie",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token cookie is missing, expired, or revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token missing");
        }

        User user = refreshTokenService.getUserFromToken(rawRefreshToken);
        String newRefreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(newRefreshToken).toString())
                .body(new RefreshResponse(newAccessToken));
    }

    @Operation(
            summary = "Log out the current session",
            description = """
                    Invalidates both the **refresh token** (cookie) and the **access token** (Bearer).

                    - The refresh token cookie is cleared (`Max-Age=0`).
                    - The access token is added to the server-side blacklist and will be rejected
                      by all subsequent requests until it naturally expires.

                    Both tokens are optional — callers may send only one or neither.

                    **CSRF protection is required:** send the value of the `XSRF-TOKEN` cookie
                    as the `X-XSRF-TOKEN` request header.
                    """,
            parameters = @Parameter(
                    in = ParameterIn.COOKIE,
                    name = "refresh_token",
                    required = false,
                    description = "HttpOnly refresh token cookie to revoke",
                    schema = @Schema(type = "string")
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully — refresh token cookie cleared"),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeForToken(rawRefreshToken);
        }

        String accessToken = extractBearerToken(authorizationHeader);
        if (accessToken != null) {
            tokenBlacklistService.blacklist(accessToken);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> responseWithRefreshCookie(AuthResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken()).toString())
                .body(new AuthResponse(response.message(), response.accessToken(), null, response.user()));
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path(refreshCookiePath)
                .maxAge(Duration.ofDays(refreshTokenDays))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path(refreshCookiePath)
                .maxAge(Duration.ZERO)
                .build();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }
}
