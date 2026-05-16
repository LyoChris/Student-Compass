package org.backendcompas.modules.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for logging in to an existing account")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "ana.popescu@student-compass.ro")
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Account password", example = "Password123!")
        @NotBlank(message = "Password is required")
        String password
) {
}
