package org.backendcompas.unit;

import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.RefreshToken;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.RefreshTokenRepository;
import org.backendcompas.modules.account.service.RefreshTokenService;
import org.backendcompas.modules.account.service.TokenHashService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final TokenHashService tokenHashService = mock(TokenHashService.class);
    private final RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, jwtUtil, tokenHashService);

    @Test
    void storeRefreshTokenPersistsHashedTokenAndExpiration() {
        User user = new User();
        String rawToken = "raw-refresh-token";
        LocalDateTime expiration = LocalDateTime.now().plusDays(7);

        when(tokenHashService.hashToken(rawToken)).thenReturn("hashed-token");
        when(jwtUtil.extractExpiration(rawToken)).thenReturn(expiration);

        service.storeRefreshToken(user, rawToken);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshTokenRevokesOldTokenAndStoresReplacement() {
        User user = userWithId();
        RefreshToken storedToken = validStoredToken(user);
        String rawToken = "old-token";

        when(tokenHashService.hashToken(rawToken)).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(storedToken));
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("new-token");
        when(tokenHashService.hashToken("new-token")).thenReturn("new-hash");
        when(jwtUtil.extractExpiration("new-token")).thenReturn(LocalDateTime.now().plusDays(7));

        String rotatedToken = service.rotateRefreshToken(rawToken);

        assertThat(rotatedToken).isEqualTo("new-token");
        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedToken);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void getUserFromTokenReturnsStoredUserForValidToken() {
        User user = userWithId();
        RefreshToken storedToken = validStoredToken(user);
        String rawToken = "valid-token";

        when(tokenHashService.hashToken(rawToken)).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(storedToken));

        assertThat(service.getUserFromToken(rawToken)).isSameAs(user);
    }

    @Test
    void revokeForTokenUpdatesStoredTokenWhenPresent() {
        RefreshToken storedToken = validStoredToken(userWithId());

        when(tokenHashService.hashToken("raw-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(storedToken));

        service.revokeForToken("raw-token");

        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void revokeForTokenDoesNothingWhenTokenMissing() {
        when(tokenHashService.hashToken("raw-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        service.revokeForToken("raw-token");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshTokenRejectsUnknownToken() {
        when(tokenHashService.hashToken("raw-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotateRefreshToken("raw-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void getUserFromTokenRejectsRevokedToken() {
        RefreshToken storedToken = validStoredToken(userWithId());
        storedToken.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        when(tokenHashService.hashToken("raw-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> service.getUserFromToken("raw-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    void getUserFromTokenRejectsExpiredToken() {
        RefreshToken storedToken = validStoredToken(userWithId());
        storedToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenHashService.hashToken("raw-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> service.getUserFromToken("raw-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has expired");
    }

    private User userWithId() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private RefreshToken validStoredToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        return token;
    }
}
