package org.backendcompas.core.exception;

import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionBuildsError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        ApiException exception = new BadRequestException("Bad input");

        ResponseEntity<ApiError> response = handler.handleApiException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bad input");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleAuthExceptionsReturnsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");

        ResponseEntity<ApiError> response = handler.handleAuthExceptions(
                new BadCredentialsException("bad"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleUsernameNotFoundReturnsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");

        ResponseEntity<ApiError> response = handler.handleAuthExceptions(
                new UsernameNotFoundException("missing"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleJwtExceptionsReturnsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure");

        ResponseEntity<ApiError> response = handler.handleJwtExceptions(new JwtException("bad"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid or expired token");
    }

    @Test
    void handleValidationAggregatesFieldErrors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/register");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        Method method = GlobalExceptionHandler.class.getDeclaredMethod(
                "handleValidation",
                MethodArgumentNotValidException.class,
                jakarta.servlet.http.HttpServletRequest.class
        );
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Email is required");
        assertThat(response.getBody().message()).contains("Password is required");
    }

    @Test
    void handleConstraintViolationReturnsBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");

        ResponseEntity<ApiError> response = handler.handleConstraintViolation(
                new ConstraintViolationException("Constraint failed", Set.of()),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Constraint failed");
    }

    @Test
    void handleGenericReturnsInternalServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
    }

    @Test
    void apiErrorRecordStoresValues() {
        Instant now = Instant.now();
        ApiError error = new ApiError(400, "Bad Request", "message", "/path", now);

        assertThat(error.status()).isEqualTo(400);
        assertThat(error.error()).isEqualTo("Bad Request");
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.path()).isEqualTo("/path");
        assertThat(error.timestamp()).isEqualTo(now);
    }

    @Test
    void exceptionSubclassesExposeStatus() {
        assertThat(new BadRequestException("x").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new ConflictException("x").getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(new NotFoundException("x").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new UnauthorizedException("x").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new ForbiddenException("x").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new MutedUserException().getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new AlreadyVotedException().getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(new DealNotFoundException("42").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new DealNotFoundException("42").getMessage()).contains("42");
    }
}
