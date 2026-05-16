package org.backendcompas.user.service;

import org.backendcompas.common.exception.BadRequestException;
import org.backendcompas.common.exception.NotFoundException;
import org.backendcompas.user.dto.UserProfileResponse;
import org.backendcompas.user.entity.ListerApprovalStatus;
import org.backendcompas.user.entity.User;
import org.backendcompas.user.entity.UserRole;
import org.backendcompas.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserAdministrationService {
    private final UserRepository userRepository;

    public UserAdministrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getPendingListers() {
        return userRepository.findByRoleAndListerApprovalStatusOrderByCreatedAtAsc(UserRole.LISTER, ListerApprovalStatus.PENDING)
                .stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional
    public UserProfileResponse updateListerApproval(UUID userId, ListerApprovalStatus approvalStatus) {
        if (approvalStatus != ListerApprovalStatus.APPROVED && approvalStatus != ListerApprovalStatus.REJECTED) {
            throw new BadRequestException("Lister approval status must be APPROVED or REJECTED");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (user.getRole() != UserRole.LISTER) {
            throw new BadRequestException("Only LISTER accounts can be moderated through this endpoint");
        }

        user.setListerApprovalStatus(approvalStatus);
        return UserProfileResponse.from(userRepository.save(user));
    }
}
