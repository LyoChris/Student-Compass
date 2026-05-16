package org.backendcompas.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.profile.dto.FixedExpenseRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileRequestDto;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class StudentProfileControllerIntegrationTest {

    private static final UUID IASI_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FII_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired private UserRepository userRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private User owner;
    private String ownerToken;
    private User otherUser;
    private String otherToken;

    @BeforeEach
    void setUp() {
        City city = cityRepository.findById(IASI_ID).orElseThrow();
        Faculty faculty = facultyRepository.findById(FII_ID).orElseThrow();

        owner = createUser("owner@test.com", city, faculty, UserRole.USER);
        ownerToken = jwtUtil.generateAccessToken(owner);

        otherUser = createUser("other@test.com", city, faculty, UserRole.USER);
        otherToken = jwtUtil.generateAccessToken(otherUser);
    }

    private StudentProfileRequestDto buildProfileDto() {
        FixedExpenseRequestDto expense = new FixedExpenseRequestDto("Rent", BigDecimal.valueOf(500));
        return StudentProfileRequestDto.builder()
                .livingArea(LivingArea.DORMITORY)
                .eatingHabit(EatingHabit.COOKING)
                .homePackageFrequency(HomePackageFrequency.MONTHLY)
                .monthlyBudget(BigDecimal.valueOf(1500))
                .fixedExpenses(List.of(expense))
                .build();
    }

    @Test
    void upsertProfileCreatesNewProfile() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.livingArea").value("DORMITORY"))
                .andExpect(jsonPath("$.monthlyBudget").value(1500));
    }

    @Test
    void upsertProfileUpdatesExistingProfile() throws Exception {
        // First create
        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isCreated());

        // Then update
        StudentProfileRequestDto updated = StudentProfileRequestDto.builder()
                .livingArea(LivingArea.RENT)
                .eatingHabit(EatingHabit.CANTEEN)
                .homePackageFrequency(HomePackageFrequency.WEEKLY)
                .monthlyBudget(BigDecimal.valueOf(2000))
                .fixedExpenses(List.of())
                .build();

        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livingArea").value("RENT"))
                .andExpect(jsonPath("$.monthlyBudget").value(2000));
    }

    @Test
    void getProfileReturnsExistingProfile() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livingArea").value("DORMITORY"));
    }

    @Test
    void getProfileNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOtherUsersProfileAsForbiddenReturns403() throws Exception {
        // Create owner's profile first
        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isCreated());

        // Other user tries to read it
        mockMvc.perform(get("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void upsertOtherUsersProfileForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyProfile() throws Exception {
        City city = cityRepository.findById(IASI_ID).orElseThrow();
        Faculty faculty = facultyRepository.findById(FII_ID).orElseThrow();
        User admin = createUser("admin@test.com", city, faculty, UserRole.ADMIN);
        String adminToken = jwtUtil.generateAccessToken(admin);

        // Create profile as owner
        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isCreated());

        // Admin reads it
        mockMvc.perform(get("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void upsertProfileWithNegativeBudgetReturns400() throws Exception {
        StudentProfileRequestDto dto = StudentProfileRequestDto.builder()
                .livingArea(LivingArea.DORMITORY)
                .eatingHabit(EatingHabit.COOKING)
                .homePackageFrequency(HomePackageFrequency.MONTHLY)
                .monthlyBudget(BigDecimal.valueOf(-100))
                .fixedExpenses(List.of())
                .build();

        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertProfileWithMissingFieldReturns400() throws Exception {
        String payload = """
                {"eatingHabit":"COOKING","homePackageFrequency":"MONTHLY","monthlyBudget":500,"fixedExpenses":[]}
                """;

        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertProfileWithEmptyExpenses() throws Exception {
        StudentProfileRequestDto dto = StudentProfileRequestDto.builder()
                .livingArea(LivingArea.OWN_HOME)
                .eatingHabit(EatingHabit.CANTEEN)
                .homePackageFrequency(HomePackageFrequency.NONE)
                .monthlyBudget(BigDecimal.valueOf(1000))
                .fixedExpenses(List.of())
                .build();

        mockMvc.perform(put("/api/v1/profiles/" + owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fixedExpenses").isArray())
                .andExpect(jsonPath("$.fixedExpenses.length()").value(0));
    }

    @Test
    void upsertProfileWithUnknownUserIdReturns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        City city = cityRepository.findById(IASI_ID).orElseThrow();
        Faculty faculty = facultyRepository.findById(FII_ID).orElseThrow();
        User admin = createUser("admin2@test.com", city, faculty, UserRole.ADMIN);
        String adminToken = jwtUtil.generateAccessToken(admin);

        mockMvc.perform(put("/api/v1/profiles/" + unknownId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProfileDto())))
                .andExpect(status().isNotFound());
    }

    private User createUser(String email, City city, Faculty faculty, UserRole role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Pass123!"));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);
        return userRepository.save(user);
    }
}
