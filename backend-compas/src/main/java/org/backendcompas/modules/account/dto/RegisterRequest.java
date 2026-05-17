package org.backendcompas.modules.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Data required to create a new student account")
public record RegisterRequest(
        @Schema(description = "First name of the student", example = "Ana")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Last name of the student", example = "Popescu")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "Student age — must be at least 18", example = "20")
        @NotNull(message = "Age is required")
        @Min(value = 18, message = "Must be at least 18")
        Integer age,

        @Schema(description = "Primary contact phone number", example = "+40722123456")
        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number must be at most 20 characters")
        @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Phone number must contain 10 to 15 digits and may start with +")
        String phoneNumber,

        @Schema(description = "Email address — must be unique across all accounts", example = "ana.popescu@student-compass.ro")
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Password — minimum 8 characters", example = "Password123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String password,

        @Schema(description = "Must match the password field exactly", example = "Password123!")
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @Schema(description = "ID of the city where the student studies — use GET /api/v1/catalog/cities",
                example = "11111111-1111-1111-1111-111111111111")
        @NotNull(message = "City is required")
        UUID cityId,

        @Schema(description = "ID of the faculty — must belong to the selected city — use GET /api/v1/catalog/cities/{cityId}/faculties",
                example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        @NotNull(message = "Faculty is required")
        UUID facultyId
) {
}
