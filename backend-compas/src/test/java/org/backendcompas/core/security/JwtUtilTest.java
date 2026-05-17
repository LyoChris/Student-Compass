package org.backendcompas.core.security;

import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generatesAndValidatesTokens() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-jwt-secret-key-must-be-at-least-32-bytes");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenMinutes", 1L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenDays", 1L);
        jwtUtil.init();

        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(jwtUtil.extractId(accessToken)).isEqualTo(user.getId());
        assertThat(jwtUtil.validateToken(accessToken).getSubject()).isEqualTo(user.getId().toString());

        LocalDateTime expiration = jwtUtil.extractExpiration(accessToken);
        assertThat(expiration).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}
