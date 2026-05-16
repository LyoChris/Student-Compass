package org.backendcompas.auth.dto.response;

import org.backendcompas.user.dto.UserProfileResponse;

public record AuthResponse(
        String message,
        String accessToken,
        String refreshToken,
        UserProfileResponse user
) {
}
