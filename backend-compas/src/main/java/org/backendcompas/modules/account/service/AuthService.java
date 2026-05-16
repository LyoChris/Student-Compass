package org.backendcompas.modules.account.service;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.ConflictException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.dto.AuthResponse;
import org.backendcompas.modules.account.dto.LoginRequest;
import org.backendcompas.modules.account.dto.RegisterRequest;
import org.backendcompas.modules.account.dto.UserProfileResponse;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;
    private static final DateTimeFormatter LOCK_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final FacultyRepository facultyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       CityRepository cityRepository,
                       FacultyRepository facultyRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.facultyRepository = facultyRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new NotFoundException("City not found: " + request.cityId()));

        Faculty faculty = facultyRepository.findByIdAndCityId(request.facultyId(), request.cityId())
                .orElseThrow(() -> new BadRequestException("Faculty must belong to the selected city"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);

        User savedUser = userRepository.save(user);
        String accessToken = jwtUtil.generateAccessToken(savedUser);
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());
        refreshTokenService.storeRefreshToken(savedUser, refreshToken);

        return new AuthResponse("Account registered successfully.", accessToken, refreshToken, UserProfileResponse.from(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        validateUserCanLogin(user);
        handleExistingLock(user);

        try {
            CustomUserDetails currentUser = (CustomUserDetails) authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
            ).getPrincipal();

            User authenticatedUser = currentUser.getUser();
            authenticatedUser.setFailedLoginAttempts(0);
            authenticatedUser.setLockedUntil(null);
            userRepository.save(authenticatedUser);

            String accessToken = jwtUtil.generateAccessToken(authenticatedUser);
            String refreshToken = jwtUtil.generateRefreshToken(authenticatedUser.getId());
            refreshTokenService.storeRefreshToken(authenticatedUser, refreshToken);

            return new AuthResponse("Login successful", accessToken, refreshToken, UserProfileResponse.from(authenticatedUser));
        } catch (AuthenticationException exception) {
            recordFailedAttempt(user);
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private void validateUserCanLogin(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is " + user.getStatus() + ". Login is not allowed.");
        }
    }

    private void handleExistingLock(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() == null) {
            return;
        }

        if (user.getLockedUntil().isAfter(now)) {
            throw new UnauthorizedException("Too many login attempts. Try again after " +
                    user.getLockedUntil().format(LOCK_TIME_FORMATTER));
        }

        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    private void recordFailedAttempt(User user) {
        int failedAttempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        failedAttempts++;
        user.setFailedLoginAttempts(failedAttempts);
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        userRepository.save(user);
    }
}
