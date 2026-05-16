package org.backendcompas.unit;

import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.controller.AuthController;
import org.backendcompas.modules.account.dto.AuthResponse;
import org.backendcompas.modules.account.dto.LoginRequest;
import org.backendcompas.modules.account.dto.RefreshResponse;
import org.backendcompas.modules.account.dto.RegisterRequest;
import org.backendcompas.modules.account.dto.UserProfileResponse;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.service.AuthService;
import org.backendcompas.modules.account.service.RefreshTokenService;
import org.backendcompas.modules.account.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerUnitTest {

    private final AuthService authService = mock(AuthService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, refreshTokenService, tokenBlacklistService, jwtUtil);
        ReflectionTestUtils.setField(controller, "secureCookies", false);
        ReflectionTestUtils.setField(controller, "refreshCookiePath", "/api/v1/auth");
        ReflectionTestUtils.setField(controller, "refreshTokenDays", 7L);
    }

    @Test
    void registerReturnsBodyWithoutRefreshTokenAndSetsCookie() {
        AuthResponse response = new AuthResponse("registered", "access-token", "refresh-token", profile());
        RegisterRequest request = new RegisterRequest("Ana", "Popescu", "ana@test.com", "Password123!", "Password123!", UUID.randomUUID(), UUID.randomUUID());
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        var entity = controller.register(request);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refresh_token=refresh-token").contains("SameSite=Lax");
        assertThat(entity.getBody().refreshToken()).isNull();
    }

    @Test
    void loginUsesConfiguredCookieSettings() {
        ReflectionTestUtils.setField(controller, "secureCookies", true);
        ReflectionTestUtils.setField(controller, "refreshCookiePath", "/custom-refresh");
        ReflectionTestUtils.setField(controller, "refreshTokenDays", 10L);
        AuthResponse response = new AuthResponse("logged-in", "access-token", "refresh-token", profile());
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        var entity = controller.login(new LoginRequest("user@test.com", "secret"));

        assertThat(entity.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=refresh-token")
                .contains("Path=/custom-refresh")
                .contains("Secure")
                .contains("SameSite=None")
                .contains("Max-Age=864000");
        assertThat(entity.getBody().refreshToken()).isNull();
    }

    @Test
    void refreshRejectsMissingOrBlankRefreshToken() {
        assertThatThrownBy(() -> controller.refresh(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token missing");
        assertThatThrownBy(() -> controller.refresh("   "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token missing");
    }

    @Test
    void refreshRotatesTokenAndReturnsNewAccessToken() {
        ReflectionTestUtils.setField(controller, "secureCookies", true);
        User user = new User();
        String oldRefreshToken = "old-refresh-token";

        when(refreshTokenService.getUserFromToken(oldRefreshToken)).thenReturn(user);
        when(refreshTokenService.rotateRefreshToken(oldRefreshToken)).thenReturn("new-refresh-token");
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");

        var entity = controller.refresh(oldRefreshToken);

        assertThat(entity.getBody()).isEqualTo(new RefreshResponse("new-access-token"));
        assertThat(entity.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=new-refresh-token")
                .contains("Secure")
                .contains("SameSite=None");
    }

    @Test
    void logoutRevokesTokensAndClearsCookie() {
        var entity = controller.logout("refresh-token", "Bearer access-token");

        verify(refreshTokenService).revokeForToken("refresh-token");
        verify(tokenBlacklistService).blacklist("access-token");
        assertThat(entity.getStatusCode().value()).isEqualTo(204);
        assertThat(entity.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=")
                .contains("Max-Age=0")
                .contains("SameSite=Lax");
    }

    @Test
    void logoutIgnoresMissingTokens() {
        var entity = controller.logout("   ", "Basic abc");

        assertThat(entity.getStatusCode().value()).isEqualTo(204);
    }

    private UserProfileResponse profile() {
        return new UserProfileResponse(
                UUID.randomUUID(),
                "Ana",
                "Popescu",
                "ana@test.com",
                "USER",
                "ACTIVE",
                UUID.randomUUID(),
                "Iasi",
                UUID.randomUUID(),
                "FII"
        );
    }
}
