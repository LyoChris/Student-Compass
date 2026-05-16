package org.backendcompas.account;

import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    private static final UUID IASI_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FII_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

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
    void adminCanAccessProtectedEndpoint() throws Exception {
        City city = cityRepository.findById(IASI_ID).orElseThrow();
        Faculty faculty = facultyRepository.findById(FII_ID).orElseThrow();

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@student-compass.test");
        admin.setPasswordHash(passwordEncoder.encode("Password123!"));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCity(city);
        admin.setFaculty(faculty);
        admin = userRepository.save(admin);

        String adminToken = jwtUtil.generateAccessToken(admin);

        mockMvc.perform(get("/api/v1/account/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@student-compass.test"));
    }
}
