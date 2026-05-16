package org.backendcompas.auth.controller;

import jakarta.validation.Valid;
import org.backendcompas.auth.dto.request.LoginRequest;
import org.backendcompas.auth.dto.request.RegisterRequest;
import org.backendcompas.auth.dto.response.AuthResponse;
import org.backendcompas.auth.dto.response.RefreshResponse;
import org.backendcompas.auth.service.AuthService;
import org.backendcompas.auth.service.RefreshTokenService;
import org.backendcompas.auth.service.TokenBlacklistService;
import org.backendcompas.common.exception.UnauthorizedException;
import org.backendcompas.security.jwt.JwtUtil;
import org.backendcompas.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return responseWithRefreshCookie(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return responseWithRefreshCookie(authService.login(request));
    }

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
