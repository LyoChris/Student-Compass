package org.backendcompas.modules.account.service;

import io.jsonwebtoken.JwtException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.RevokedAccessToken;
import org.backendcompas.modules.account.repository.RevokedAccessTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenHashService tokenHashService;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void isRevokedDelegatesToRepository() {
        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(revokedAccessTokenRepository.existsByTokenHash("hash")).thenReturn(true);

        boolean result = tokenBlacklistService.isRevoked("raw");

        assertThat(result).isTrue();
    }

    @Test
    void blacklistIgnoresExpiredToken() {
        when(jwtUtil.extractExpiration("raw")).thenReturn(LocalDateTime.now().minusMinutes(1));

        tokenBlacklistService.blacklist("raw");

        verify(revokedAccessTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blacklistIgnoresExistingTokenHash() {
        when(jwtUtil.extractExpiration("raw")).thenReturn(LocalDateTime.now().plusMinutes(5));
        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(revokedAccessTokenRepository.existsByTokenHash("hash")).thenReturn(true);

        tokenBlacklistService.blacklist("raw");

        verify(revokedAccessTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blacklistStoresNewTokenHash() {
        when(jwtUtil.extractExpiration("raw")).thenReturn(LocalDateTime.now().plusMinutes(5));
        when(tokenHashService.hashToken("raw")).thenReturn("hash");
        when(revokedAccessTokenRepository.existsByTokenHash("hash")).thenReturn(false);

        tokenBlacklistService.blacklist("raw");

        ArgumentCaptor<RevokedAccessToken> captor = ArgumentCaptor.forClass(RevokedAccessToken.class);
        verify(revokedAccessTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hash");
    }

    @Test
    void blacklistIgnoresJwtExceptions() {
        when(jwtUtil.extractExpiration("raw")).thenThrow(new JwtException("bad"));

        tokenBlacklistService.blacklist("raw");

        verify(revokedAccessTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blacklistIgnoresIllegalArgumentException() {
        when(jwtUtil.extractExpiration("raw")).thenThrow(new IllegalArgumentException("bad"));

        tokenBlacklistService.blacklist("raw");

        verify(revokedAccessTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
