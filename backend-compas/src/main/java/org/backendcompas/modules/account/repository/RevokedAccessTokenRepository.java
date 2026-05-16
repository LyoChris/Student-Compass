package org.backendcompas.modules.account.repository;

import org.backendcompas.modules.account.model.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, UUID> {
    boolean existsByTokenHash(String tokenHash);
}
