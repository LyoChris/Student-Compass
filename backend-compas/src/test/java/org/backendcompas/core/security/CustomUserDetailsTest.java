package org.backendcompas.core.security;

import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void exposesUserInformation() {
        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);
        user.setLockedUntil(null);

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUser()).isEqualTo(user);
        assertThat(details.getUserId()).isEqualTo(user.getId());
        assertThat(details.getUsername()).isEqualTo("user@test.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void accountLockedWhenLockedUntilInFuture() {
        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void disabledWhenStatusNotActive() {
        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.SUSPENDED);
        user.setCity(city);
        user.setFaculty(faculty);

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isFalse();
    }
}
