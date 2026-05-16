package org.backendcompas.core.config;

import org.backendcompas.core.security.JwtAccessDeniedHandler;
import org.backendcompas.core.security.JwtAuthenticationEntryPoint;
import org.backendcompas.core.security.JwtAuthenticationFilter;
import org.backendcompas.modules.account.model.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final DaoAuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          DaoAuthenticationProvider authenticationProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .requireCsrfProtectionMatcher(request -> {
                            String path = request.getServletPath();
                            String method = request.getMethod();
                            return "POST".equalsIgnoreCase(method)
                                    && ("/api/v1/auth/refresh".equals(path) || "/api/v1/auth/logout".equals(path));
                        })
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/api/v1/catalog/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole(UserRole.ADMIN.name())
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
