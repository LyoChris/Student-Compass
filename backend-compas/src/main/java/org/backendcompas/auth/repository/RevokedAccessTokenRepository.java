package org.backendcompas.auth.repository;

import org.backendcompas.auth.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, UUID> {
    boolean existsByTokenHash(String tokenHash);
}
