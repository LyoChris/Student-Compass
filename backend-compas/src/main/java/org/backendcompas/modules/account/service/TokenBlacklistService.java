package org.backendcompas.modules.account.service;

import io.jsonwebtoken.JwtException;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.RevokedAccessToken;
import org.backendcompas.modules.account.repository.RevokedAccessTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenBlacklistService {
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final JwtUtil jwtUtil;
    private final TokenHashService tokenHashService;

    public TokenBlacklistService(RevokedAccessTokenRepository revokedAccessTokenRepository,
                                 JwtUtil jwtUtil,
                                 TokenHashService tokenHashService) {
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.jwtUtil = jwtUtil;
        this.tokenHashService = tokenHashService;
    }

    public boolean isRevoked(String rawToken) {
        return revokedAccessTokenRepository.existsByTokenHash(tokenHashService.hashToken(rawToken));
    }

    public void blacklist(String rawToken) {
        try {
            LocalDateTime expiresAt = jwtUtil.extractExpiration(rawToken);
            if (expiresAt.isBefore(LocalDateTime.now())) {
                return;
            }

            String tokenHash = tokenHashService.hashToken(rawToken);
            if (revokedAccessTokenRepository.existsByTokenHash(tokenHash)) {
                return;
            }

            RevokedAccessToken entity = new RevokedAccessToken();
            entity.setTokenHash(tokenHash);
            entity.setExpiresAt(expiresAt);
            revokedAccessTokenRepository.save(entity);
        } catch (JwtException | IllegalArgumentException exception) {
            // Ignore malformed or expired tokens on logout. The refresh token cleanup is still applied.
        }
    }
}
