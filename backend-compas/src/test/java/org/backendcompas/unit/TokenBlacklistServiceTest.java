package org.backendcompas.unit;

import io.jsonwebtoken.JwtException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.repository.RevokedAccessTokenRepository;
import org.backendcompas.modules.account.service.TokenBlacklistService;
import org.backendcompas.modules.account.service.TokenHashService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    private final RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final TokenHashService tokenHashService = mock(TokenHashService.class);
    private final TokenBlacklistService service = new TokenBlacklistService(revokedAccessTokenRepository, jwtUtil, tokenHashService);

    @Test
    void isRevokedDelegatesToRepository() {
        when(tokenHashService.hashToken("token")).thenReturn("hash");
        when(revokedAccessTokenRepository.existsByTokenHash("hash")).thenReturn(true);

        assertThat(service.isRevoked("token")).isTrue();
    }

    @Test
    void blacklistSkipsExpiredToken() {
        when(jwtUtil.extractExpiration("token")).thenReturn(LocalDateTime.now().minusMinutes(1));

        service.blacklist("token");

        verify(revokedAccessTokenRepository, never()).save(any());
    }

    @Test
    void blacklistSkipsAlreadyRevokedToken() {
        when(jwtUtil.extractExpiration("token")).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(tokenHashService.hashToken("token")).thenReturn("hash");
        when(revokedAccessTokenRepository.existsByTokenHash("hash")).thenReturn(true);

        service.blacklist("token");

        verify(revokedAccessTokenRepository, never()).save(any());
    }

    @Test
    void blacklistStoresActiveTokenWhenNotPresent() {
        when(jwtUtil.extractExpiration("token")).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(tokenHashService.hashToken("token")).thenReturn("hash");
        when(revokedAccessTokenRepository.existsByTokenHash("hash")).thenReturn(false);

        service.blacklist("token");

        verify(revokedAccessTokenRepository).save(any());
    }

    @Test
    void blacklistIgnoresMalformedToken() {
        when(jwtUtil.extractExpiration("token")).thenThrow(new JwtException("bad token"));

        service.blacklist("token");

        verify(revokedAccessTokenRepository, never()).save(any());
    }
}
