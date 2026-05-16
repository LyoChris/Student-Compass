package org.backendcompas.modules.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after a successful login or registration")
public record AuthResponse(
        @Schema(description = "Human-readable confirmation message", example = "Account registered successfully.")
        String message,

        @Schema(description = "Short-lived JWT access token (expires in 10 min). Send as Bearer in the Authorization header.")
        String accessToken,

        @Schema(description = "Always null in the response body — the refresh token is transported via the HttpOnly 'refresh_token' cookie.",
                nullable = true, example = "null")
        String refreshToken,

        @Schema(description = "Profile of the authenticated user")
        UserProfileResponse user
) {
}
