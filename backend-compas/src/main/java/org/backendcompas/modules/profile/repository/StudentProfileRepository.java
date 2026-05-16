package org.backendcompas.modules.profile.repository;

import org.backendcompas.modules.profile.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
}
