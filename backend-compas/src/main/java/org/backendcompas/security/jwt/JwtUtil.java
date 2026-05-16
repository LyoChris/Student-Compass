package org.backendcompas.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.backendcompas.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-minutes:10}")
    private long accessTokenMinutes;

    @Value("${app.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("cityId", user.getCity().getId().toString())
                .claim("facultyId", user.getFaculty().getId().toString())
                .claim("listerApprovalStatus", user.getListerApprovalStatus().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenMinutes * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenDays * 24 * 60 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractId(String token) {
        return UUID.fromString(validateToken(token).getSubject());
    }

    public LocalDateTime extractExpiration(String token) {
        return validateToken(token).getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
