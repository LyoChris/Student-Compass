package org.backendcompas.user.repository;

import org.backendcompas.user.entity.ListerApprovalStatus;
import org.backendcompas.user.entity.User;
import org.backendcompas.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleAndListerApprovalStatusOrderByCreatedAtAsc(UserRole role, ListerApprovalStatus approvalStatus);
}
