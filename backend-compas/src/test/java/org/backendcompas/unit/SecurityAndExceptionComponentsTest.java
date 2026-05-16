package org.backendcompas.unit;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.GlobalExceptionHandler;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.core.security.CustomUserDetailsService;
import org.backendcompas.core.security.JwtAccessDeniedHandler;
import org.backendcompas.core.security.JwtAuthenticationFilter;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.account.service.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityAndExceptionComponentsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customUserDetailsExposesUserStateAcrossBranches() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);

        CustomUserDetails details = new CustomUserDetails(user);
        assertThat(details.getUser()).isSameAs(user);
        assertThat(details.getUserId()).isEqualTo(user.getId());
        assertThat(details.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(details.getUsername()).isEqualTo("user@test.com");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();

        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        user.setStatus(UserStatus.SUSPENDED);

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isEnabled()).isFalse();

        user.setLockedUntil(LocalDateTime.now().minusMinutes(5));
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void customUserDetailsServiceLoadsByEmailAndId() {
        UserRepository userRepository = mock(UserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        User user = user();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("user@test.com")).isInstanceOf(CustomUserDetails.class);
        assertThat(service.loadUserById(user.getId()).getUserId()).isEqualTo(user.getId());
    }

    @Test
    void customUserDetailsServiceThrowsWhenUserMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UUID userId = UUID.randomUUID();

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with email: missing@test.com");
        assertThatThrownBy(() -> service.loadUserById(userId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    @Test
    void jwtAuthenticationFilterSkipsAuthEndpoints() {
        ExposedJwtAuthenticationFilter filter = new ExposedJwtAuthenticationFilter(
                mock(JwtUtil.class),
                mock(CustomUserDetailsService.class),
                mock(TokenBlacklistService.class)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");

        assertThat(filter.exposedShouldNotFilter(request)).isTrue();
    }

    @Test
    void jwtAuthenticationFilterSetsAuthenticationForValidBearerToken() throws ServletException, IOException {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);
        User user = user();
        CustomUserDetails details = new CustomUserDetails(user);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");

        when(blacklistService.isRevoked("token")).thenReturn(false);
        when(jwtUtil.extractId("token")).thenReturn(user.getId());
        when(userDetailsService.loadUserById(user.getId())).thenReturn(details);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void jwtAuthenticationFilterLeavesContextEmptyForRevokedToken() throws ServletException, IOException {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer revoked-token");

        when(blacklistService.isRevoked("revoked-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void jwtAuthenticationFilterClearsContextOnRuntimeException() throws ServletException, IOException {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, blacklistService);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer bad-token");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("existing", null));

        when(blacklistService.isRevoked("bad-token")).thenReturn(false);
        when(jwtUtil.extractId("bad-token")).thenThrow(new IllegalArgumentException("bad token"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void jwtAccessDeniedHandlerWritesForbiddenJson() throws Exception {
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":403").contains("/api/v1/account/me");
    }

    @Test
    void globalExceptionHandlerMapsKnownExceptions() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        ResponseEntity<ApiError> apiError = handler.handleApiException(new BadRequestException("bad request"), request);
        ResponseEntity<ApiError> accessDenied = handler.handleAccessDenied(new AccessDeniedException("denied"), request);
        ResponseEntity<ApiError> auth = handler.handleAuthExceptions(new BadCredentialsException("bad creds"), request);
        ResponseEntity<ApiError> jwt = handler.handleJwtExceptions(new JwtException("bad token"), request);
        ResponseEntity<ApiError> constraint = handler.handleConstraintViolation(
                new jakarta.validation.ConstraintViolationException("constraint failed", Set.of()),
                request
        );
        ResponseEntity<ApiError> generic = handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(apiError.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(apiError.getBody().message()).isEqualTo("bad request");
        assertThat(accessDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(auth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jwt.getBody().message()).isEqualTo("Invalid or expired token");
        assertThat(constraint.getBody().message()).isEqualTo("constraint failed");
        assertThat(generic.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void globalExceptionHandlerCollectsValidationMessages() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Email is required; Password is required");
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static final class ExposedJwtAuthenticationFilter extends JwtAuthenticationFilter {

        private ExposedJwtAuthenticationFilter(JwtUtil jwtUtil,
                                               CustomUserDetailsService customUserDetailsService,
                                               TokenBlacklistService tokenBlacklistService) {
            super(jwtUtil, customUserDetailsService, tokenBlacklistService);
        }

        private boolean exposedShouldNotFilter(HttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
