package org.backendcompas.user.dto;

import org.backendcompas.user.entity.User;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        String listerApprovalStatus,
        UUID cityId,
        String cityName,
        UUID facultyId,
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
                user.getListerApprovalStatus().name(),
                user.getCity().getId(),
                user.getCity().getName(),
                user.getFaculty().getId(),
                user.getFaculty().getName()
        );
    }
}
