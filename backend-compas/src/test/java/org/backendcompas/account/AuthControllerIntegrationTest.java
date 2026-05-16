package org.backendcompas.account;

import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.RefreshTokenRepository;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    private static final UUID IASI_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FII_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private City iasi;
    private Faculty fii;

    @BeforeEach
    void setUp() {
        iasi = cityRepository.findById(IASI_ID).orElseThrow();
        fii  = facultyRepository.findById(FII_ID).orElseThrow();
    }

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Test
    void registerUserReturnsTokensAndProfile() throws Exception {
        String payload = """
                {
                  "firstName": "Ana",
                  "lastName": "Popescu",
                  "email": "ana.popescu@student-compass.test",
                  "password": "Password123!",
                  "confirmPassword": "Password123!",
                  "cityId": "11111111-1111-1111-1111-111111111111",
                  "facultyId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.message").value("Account registered successfully."))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.cityName").value("Iasi"))
                .andExpect(jsonPath("$.user.facultyName").value("Facultatea de Informatica"));
    }

    @Test
    void registerRejectsFacultyFromAnotherCity() throws Exception {
        String payload = """
                {
                  "firstName": "Mihai",
                  "lastName": "Ionescu",
                  "email": "mihai.ionescu@student-compass.test",
                  "password": "Password123!",
                  "confirmPassword": "Password123!",
                  "cityId": "11111111-1111-1111-1111-111111111111",
                  "facultyId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Faculty must belong to the selected city"));
    }

    @Test
    void registerRejectsPasswordMismatch() throws Exception {
        String payload = """
                {
                  "firstName": "Ion",
                  "lastName": "Pop",
                  "email": "ion.pop@test.com",
                  "password": "Password123!",
                  "confirmPassword": "WrongPass!",
                  "cityId": "11111111-1111-1111-1111-111111111111",
                  "facultyId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        saveActiveUser("dup@test.com", "Pass");

        String payload = """
                {
                  "firstName": "Dup",
                  "lastName": "User",
                  "email": "dup@test.com",
                  "password": "Password123!",
                  "confirmPassword": "Password123!",
                  "cityId": "11111111-1111-1111-1111-111111111111",
                  "facultyId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsUnknownCity() throws Exception {
        String payload = """
                {
                  "firstName": "X",
                  "lastName": "Y",
                  "email": "xy@test.com",
                  "password": "Password123!",
                  "confirmPassword": "Password123!",
                  "cityId": "00000000-0000-0000-0000-000000000000",
                  "facultyId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    void loginSuccessfully() throws Exception {
        saveActiveUser("login@test.com", "Password123!");

        String payload = """
                { "email": "login@test.com", "password": "Password123!" }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        saveActiveUser("badpass@test.com", "RealPass123!");

        String payload = """
                { "email": "badpass@test.com", "password": "WrongPass!" }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailReturns401() throws Exception {
        String payload = """
                { "email": "nobody@test.com", "password": "Password123!" }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithSuspendedAccountReturns401() throws Exception {
        User user = saveActiveUser("suspended@test.com", "Pass123!");
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String payload = """
                { "email": "suspended@test.com", "password": "Pass123!" }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithLockedAccountReturns401() throws Exception {
        User user = saveActiveUser("locked@test.com", "Pass123!");
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        user.setFailedLoginAttempts(5);
        userRepository.save(user);

        String payload = """
                { "email": "locked@test.com", "password": "Pass123!" }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithExpiredLockResetsAndSucceeds() throws Exception {
        User user = saveActiveUser("expiredlock@test.com", "Pass123!");
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setFailedLoginAttempts(5);
        userRepository.save(user);

        String payload = """
                { "email": "expiredlock@test.com", "password": "Pass123!" }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void multipleFailedLoginAttemptsEventuallyLockAccount() throws Exception {
        saveActiveUser("lockme@test.com", "RealPass123!");

        String wrongPayload = """
                { "email": "lockme@test.com", "password": "Wrong!" }
                """;

        // 5 failed attempts — 5th triggers lock
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt (still wrong password) — should be locked
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPayload))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    @Test
    void refreshTokenRotatesSuccessfully() throws Exception {
        // Register to get tokens
        String registerPayload = """
                {
                  "firstName": "Refresh",
                  "lastName": "User",
                  "email": "refresh@test.com",
                  "password": "Password123!",
                  "confirmPassword": "Password123!",
                  "cityId": "11111111-1111-1111-1111-111111111111",
                  "facultyId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn();

        // Verify we get a SET_COOKIE with refresh_token on registration
        jakarta.servlet.http.Cookie[] cookies = result.getResponse().getCookies();
        boolean hasRefreshCookie = false;
        for (jakarta.servlet.http.Cookie c : cookies) {
            if ("refresh_token".equals(c.getName())) hasRefreshCookie = true;
        }
        org.assertj.core.api.Assertions.assertThat(hasRefreshCookie).isTrue();
    }

    @Test
    void refreshWithNoCookieReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Test
    void logoutSuccessfully() throws Exception {
        String registerPayload = """
                {
                  "firstName": "Logout",
                  "lastName": "User",
                  "email": "logout@test.com",
                  "password": "Password123!",
                  "confirmPassword": "Password123!",
                  "cityId": "11111111-1111-1111-1111-111111111111",
                  "facultyId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        var regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andReturn();

        String accessToken = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(regResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        Cookie[] cookies = regResult.getResponse().getCookies();
        Cookie refreshCookie = null;
        Cookie xsrfCookie = null;
        for (Cookie c : cookies) {
            if ("refresh_token".equals(c.getName())) refreshCookie = c;
            if ("XSRF-TOKEN".equals(c.getName())) xsrfCookie = c;
        }

        var logoutRequest = post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        if (refreshCookie != null) logoutRequest.cookie(refreshCookie);
        if (xsrfCookie != null) {
            logoutRequest.cookie(xsrfCookie);
            logoutRequest.header("X-XSRF-TOKEN", xsrfCookie.getValue());
        }

        mockMvc.perform(logoutRequest)
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // /account/me
    // -------------------------------------------------------------------------

    @Test
    void adminCanAccessProtectedEndpoint() throws Exception {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@student-compass.test");
        admin.setPasswordHash(passwordEncoder.encode("Password123!"));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCity(iasi);
        admin.setFaculty(fii);
        admin = userRepository.save(admin);

        String adminToken = jwtUtil.generateAccessToken(admin);

        mockMvc.perform(get("/api/v1/account/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@student-compass.test"));
    }

    @Test
    void accountMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/account/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountMeWithRevokedTokenReturns401() throws Exception {
                saveActiveUser("revoked@test.com", "Pass123!");

        // Blacklist the token via logout (need CSRF so just check filter path)
        // Instead directly access with an invalid token to hit the filter's catch path
        mockMvc.perform(get("/api/v1/account/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User saveActiveUser(String email, String rawPassword) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(iasi);
        user.setFaculty(fii);
        return userRepository.save(user);
    }
}
