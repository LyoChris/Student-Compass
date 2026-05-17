package org.backendcompas.modules.account.service;

import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.RefreshToken;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenHashService tokenHashService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void storeRefreshTokenPersistsHashAndExpiry() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(jwtUtil.extractExpiration("raw")).thenReturn(LocalDateTime.now().plusDays(1));

        refreshTokenService.storeRefreshToken(user, "raw");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hash");
    }

    @Test
    void getUserFromTokenReturnsUserWhenValid() {
        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        User result = refreshTokenService.getUserFromToken("raw");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserFromTokenRejectsMissingToken() {
        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getUserFromToken("raw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void getUserFromTokenRejectsRevokedToken() {
        RefreshToken token = new RefreshToken();
        token.setTokenHash("hash");
        token.setRevokedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.getUserFromToken("raw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    void getUserFromTokenRejectsExpiredToken() {
        RefreshToken token = new RefreshToken();
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.getUserFromToken("raw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has expired");
    }

    @Test
    void rotateRefreshTokenRevokesOldAndStoresNew() {
        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("hash");
        token.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("new");
        when(jwtUtil.extractExpiration("new")).thenReturn(LocalDateTime.now().plusDays(2));

        String result = refreshTokenService.rotateRefreshToken("raw");

        assertThat(result).isEqualTo("new");
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    void revokeForTokenMarksTokenRevoked() {
        RefreshToken token = new RefreshToken();
        token.setTokenHash("hash");

        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        refreshTokenService.revokeForToken("raw");

        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }
}
