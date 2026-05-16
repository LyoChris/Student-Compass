package org.backendcompas.modules.account.service;

import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.modules.account.model.RefreshToken;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final TokenHashService tokenHashService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtUtil jwtUtil,
                               TokenHashService tokenHashService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.tokenHashService = tokenHashService;
    }

    public void storeRefreshToken(User user, String rawToken) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(tokenHashService.hashToken(rawToken));
        entity.setExpiresAt(jwtUtil.extractExpiration(rawToken));
        refreshTokenRepository.save(entity);
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken storedToken = getValidStoredToken(rawToken);
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        String newRawToken = jwtUtil.generateRefreshToken(storedToken.getUser().getId());
        storeRefreshToken(storedToken.getUser(), newRawToken);
        return newRawToken;
    }

    @Transactional(readOnly = true)
    public User getUserFromToken(String rawToken) {
        return getValidStoredToken(rawToken).getUser();
    }

    @Transactional
    public void revokeForToken(String rawToken) {
        String tokenHash = tokenHashService.hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    private RefreshToken getValidStoredToken(String rawToken) {
        jwtUtil.validateToken(rawToken);
        String tokenHash = tokenHashService.hashToken(rawToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getRevokedAt() != null) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        return storedToken;
    }
}
