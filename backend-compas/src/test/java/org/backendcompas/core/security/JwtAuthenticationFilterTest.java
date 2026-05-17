package org.backendcompas.core.security;

import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.service.TokenBlacklistService;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsFilteringForAuthEndpoints() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void stopsWhenTokenIsRevoked() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(blacklistService.isRevoked("token")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserById(Mockito.any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void setsAuthenticationForValidToken() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);

        UUID userId = UUID.randomUUID();
        when(jwtUtil.extractId("token")).thenReturn(userId);
        when(blacklistService.isRevoked("token")).thenReturn(false);

        City city = new City();
        city.setId(UUID.randomUUID());
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);

        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        when(userDetailsService.loadUserById(userId)).thenReturn(userDetails);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        verify(chain).doFilter(request, response);
    }

    @Test
    void clearsContextOnTokenParsingError() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(blacklistService.isRevoked("token")).thenReturn(false);
        when(jwtUtil.extractId("token")).thenThrow(new IllegalArgumentException("bad"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
