package org.backendcompas.modules.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "New access token issued after a successful token refresh")
public record RefreshResponse(
        @Schema(description = "New short-lived JWT access token (expires in 10 min)")
        String accessToken
) {
}
