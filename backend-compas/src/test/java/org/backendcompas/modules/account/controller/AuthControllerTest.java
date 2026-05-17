package org.backendcompas.modules.account.controller;

import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.dto.AuthResponse;
import org.backendcompas.modules.account.dto.LoginRequest;
import org.backendcompas.modules.account.dto.RefreshResponse;
import org.backendcompas.modules.account.dto.RegisterRequest;
import org.backendcompas.modules.account.dto.UserProfileResponse;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.service.AuthService;
import org.backendcompas.modules.account.service.RefreshTokenService;
import org.backendcompas.modules.account.service.TokenBlacklistService;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthService authService;
    private RefreshTokenService refreshTokenService;
    private TokenBlacklistService tokenBlacklistService;
    private JwtUtil jwtUtil;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        refreshTokenService = Mockito.mock(RefreshTokenService.class);
        tokenBlacklistService = Mockito.mock(TokenBlacklistService.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        controller = new AuthController(authService, refreshTokenService, tokenBlacklistService, jwtUtil);

        ReflectionTestUtils.setField(controller, "secureCookies", false);
        ReflectionTestUtils.setField(controller, "refreshCookiePath", "/api/v1/auth");
        ReflectionTestUtils.setField(controller, "refreshTokenDays", 7L);
    }

    @Test
    void registerReturnsRefreshCookieAndNullBodyToken() {
        RegisterRequest request = new RegisterRequest(
                "Ana",
                "Popescu",
                20,
                "+40722123456",
                "ana@test.com",
                "Password123!",
                "Password123!",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        UserProfileResponse profile = new UserProfileResponse(
                UUID.randomUUID(), "Ana", "Popescu", 20, "+40722123456", "ana@test.com",
                "USER", "ACTIVE", UUID.randomUUID(), "Iasi", UUID.randomUUID(), "FII"
        );

        when(authService.register(request)).thenReturn(
                new AuthResponse("Account registered successfully.", "access", "refresh", profile)
        );

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refresh_token=refresh");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().refreshToken()).isNull();
    }

    @Test
    void loginReturnsRefreshCookie() {
        LoginRequest request = new LoginRequest("ana@test.com", "Password123!");
        UserProfileResponse profile = new UserProfileResponse(
                UUID.randomUUID(), "Ana", "Popescu", 20, "+40722123456", "ana@test.com",
                "USER", "ACTIVE", UUID.randomUUID(), "Iasi", UUID.randomUUID(), "FII"
        );

        when(authService.login(request)).thenReturn(
                new AuthResponse("Login successful", "access", "refresh", profile)
        );

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refresh_token=refresh");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().refreshToken()).isNull();
    }

    @Test
    void refreshRejectsMissingToken() {
        assertThatThrownBy(() -> controller.refresh(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token missing");
    }

    @Test
    void refreshRotatesTokenAndReturnsAccessToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        City city = new City();
        city.setId(UUID.randomUUID());
        city.setName("Iasi");
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);
        faculty.setName("FII");
        user.setCity(city);
        user.setFaculty(faculty);

        when(refreshTokenService.getUserFromToken("refresh"))
                .thenReturn(user);
        when(refreshTokenService.rotateRefreshToken("refresh"))
                .thenReturn("newRefresh");
        when(jwtUtil.generateAccessToken(user)).thenReturn("newAccess");

        ResponseEntity<RefreshResponse> response = controller.refresh("refresh");

        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=newRefresh");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("newAccess");
    }

    @Test
    void logoutRevokesTokensAndClearsCookie() {
        ResponseEntity<Void> response = controller.logout("refresh", "Bearer access");

        verify(refreshTokenService).revokeForToken("refresh");
        verify(tokenBlacklistService).blacklist("access");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void logoutWithoutTokensDoesNotCallServices() {
        ResponseEntity<Void> response = controller.logout(null, null);

                verify(refreshTokenService, never()).revokeForToken(Mockito.anyString());
                verify(tokenBlacklistService, never()).blacklist(Mockito.anyString());
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }
}
