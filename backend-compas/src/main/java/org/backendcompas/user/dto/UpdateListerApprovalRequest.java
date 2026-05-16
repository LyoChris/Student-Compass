package org.backendcompas.user.dto;

import jakarta.validation.constraints.NotNull;
import org.backendcompas.user.entity.ListerApprovalStatus;

public record UpdateListerApprovalRequest(@NotNull ListerApprovalStatus approvalStatus) {
}
