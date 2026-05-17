package org.backendcompas.core.security;

import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Test
    void loadsUserByUsername() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("user@test.com");

        assertThat(details.getUser()).isEqualTo(user);
    }

    @Test
    void throwsWhenUserNotFoundByUsername() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@test.com");
    }

    @Test
    void loadsUserById() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        CustomUserDetails details = service.loadUserById(user.getId());

        assertThat(details.getUser()).isEqualTo(user);
    }

    @Test
    void throwsWhenUserNotFoundById() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(id))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
