package org.backendcompas.core.config;

import org.backendcompas.core.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SecurityBeansConfigTest {

    @Test
    void createsPasswordEncoderAndAuthenticates() {
        SecurityBeansConfig config = new SecurityBeansConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        CustomUserDetailsService userDetailsService = Mockito.mock(CustomUserDetailsService.class);
        String rawPassword = "Secret123!";
        UserDetails details = User.withUsername("user")
                .password(encoder.encode(rawPassword))
                .authorities("ROLE_USER")
                .build();

        when(userDetailsService.loadUserByUsername("user")).thenReturn(details);

        DaoAuthenticationProvider provider = config.authenticationProvider(userDetailsService, encoder);
        AuthenticationManager manager = config.authenticationManager(provider);

        Authentication result = manager.authenticate(
                new UsernamePasswordAuthenticationToken("user", rawPassword)
        );

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("user");
    }
}
