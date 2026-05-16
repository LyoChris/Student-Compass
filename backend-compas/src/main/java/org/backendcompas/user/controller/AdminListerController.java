package org.backendcompas.user.controller;

import jakarta.validation.Valid;
import org.backendcompas.user.dto.UpdateListerApprovalRequest;
import org.backendcompas.user.dto.UserProfileResponse;
import org.backendcompas.user.service.UserAdministrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/listers")
public class AdminListerController {
    private final UserAdministrationService userAdministrationService;

    public AdminListerController(UserAdministrationService userAdministrationService) {
        this.userAdministrationService = userAdministrationService;
    }

    @GetMapping("/pending")
    public List<UserProfileResponse> pendingListers() {
        return userAdministrationService.getPendingListers();
    }

    @PatchMapping("/{userId}/approval")
    public UserProfileResponse updateApproval(@PathVariable UUID userId,
                                              @Valid @RequestBody UpdateListerApprovalRequest request) {
        return userAdministrationService.updateListerApproval(userId, request.approvalStatus());
    }
}
