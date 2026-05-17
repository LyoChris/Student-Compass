package org.backendcompas.modules.account.service;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.ConflictException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.dto.AuthResponse;
import org.backendcompas.modules.account.dto.RegisterRequest;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsPasswordMismatch() {
        RegisterRequest request = buildRegisterRequest("pass", "different");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Passwords do not match");
    }

    @Test
    void registerRejectsExistingEmail() {
        RegisterRequest request = buildRegisterRequest("pass", "pass");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(request.email());
    }

    @Test
    void registerRejectsMissingCity() {
        RegisterRequest request = buildRegisterRequest("pass", "pass");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(cityRepository.findById(request.cityId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("City not found");
    }

    @Test
    void registerRejectsFacultyOutsideCity() {
        RegisterRequest request = buildRegisterRequest("pass", "pass");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        City city = new City();
        city.setId(request.cityId());
        when(cityRepository.findById(request.cityId())).thenReturn(Optional.of(city));
        when(facultyRepository.findByIdAndCityId(request.facultyId(), request.cityId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Faculty must belong to the selected city");
    }

    @Test
    void registerCreatesUserAndTokens() {
        RegisterRequest request = buildRegisterRequest("pass", "pass");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        City city = new City();
        city.setId(request.cityId());
        city.setName("Iasi");
        when(cityRepository.findById(request.cityId())).thenReturn(Optional.of(city));

        Faculty faculty = new Faculty();
        faculty.setId(request.facultyId());
        faculty.setCity(city);
        faculty.setName("FII");
        when(facultyRepository.findByIdAndCityId(request.facultyId(), request.cityId()))
                .thenReturn(Optional.of(faculty));

        when(passwordEncoder.encode("pass")).thenReturn("hash");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setCity(city);
        savedUser.setFaculty(faculty);
        savedUser.setRole(UserRole.USER);
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setEmail(request.email());
        savedUser.setFirstName(request.firstName());
        savedUser.setLastName(request.lastName());
        savedUser.setAge(request.age());
        savedUser.setPhoneNumber(request.phoneNumber());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken(savedUser)).thenReturn("access");
        when(jwtUtil.generateRefreshToken(savedUser.getId())).thenReturn("refresh");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.message()).contains("Account registered successfully");

        verify(refreshTokenService).storeRefreshToken(savedUser, "refresh");
    }

    @Test
    void loginRejectsInactiveUser() {
        User user = baseUser();
        user.setStatus(UserStatus.SUSPENDED);
        var request = new org.backendcompas.modules.account.dto.LoginRequest("user@test.com", "pass");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Account is");
    }

    @Test
    void loginRejectsLockedUser() {
        User user = baseUser();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        var request = new org.backendcompas.modules.account.dto.LoginRequest("user@test.com", "pass");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    void loginResetsExpiredLockAndAuthenticates() {
        User user = baseUser();
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        CustomUserDetails details = new CustomUserDetails(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        when(jwtUtil.generateAccessToken(user)).thenReturn("access");
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("refresh");

        AuthResponse response = authService.login(new org.backendcompas.modules.account.dto.LoginRequest("user@test.com", "pass"));

        assertThat(response.accessToken()).isEqualTo("access");
        verify(refreshTokenService).storeRefreshToken(user, "refresh");
    }

    @Test
    void loginRecordsFailedAttemptOnBadCredentials() {
        User user = baseUser();
        var request = new org.backendcompas.modules.account.dto.LoginRequest("user@test.com", "pass");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(1);
    }

    private RegisterRequest buildRegisterRequest(String password, String confirm) {
        return new RegisterRequest(
                "Ana",
                "Popescu",
                20,
                "+40722123456",
                "user@test.com",
                password,
                confirm,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        );
    }

    private User baseUser() {
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
        user.setFailedLoginAttempts(0);
        return user;
    }
}
