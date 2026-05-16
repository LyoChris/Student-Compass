package org.backendcompas.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.backendcompas.user.entity.UserRole;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String password,
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,
        @NotNull(message = "Role is required")
        UserRole role,
        @NotNull(message = "City is required")
        UUID cityId,
        @NotNull(message = "Faculty is required")
        UUID facultyId
) {
}
