package org.backendcompas.modules.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.account.model.User;

import java.util.UUID;

@Schema(description = "Public profile of the authenticated user")
public record UserProfileResponse(
        @Schema(description = "Unique user identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "First name", example = "Ana")
        String firstName,

        @Schema(description = "Last name", example = "Popescu")
        String lastName,

        @Schema(description = "Email address", example = "ana.popescu@student-compass.ro")
        String email,

        @Schema(description = "Account role — USER or ADMIN", example = "USER", allowableValues = {"USER", "ADMIN"})
        String role,

        @Schema(description = "Account status — ACTIVE or LOCKED", example = "ACTIVE", allowableValues = {"ACTIVE", "LOCKED"})
        String status,

        @Schema(description = "ID of the city associated with this account", example = "11111111-1111-1111-1111-111111111111")
        UUID cityId,

        @Schema(description = "Display name of the city", example = "Iasi")
        String cityName,

        @Schema(description = "ID of the faculty associated with this account", example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        UUID facultyId,

        @Schema(description = "Display name of the faculty", example = "Facultatea de Informatica")
        String facultyName
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCity().getId(),
                user.getCity().getName(),
                user.getFaculty().getId(),
                user.getFaculty().getName()
        );
    }
}
