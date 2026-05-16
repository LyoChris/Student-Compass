package org.backendcompas.modules.profile.service;

import org.backendcompas.modules.profile.dto.StudentProfileRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileResponseDto;

import java.util.UUID;

public interface StudentProfileService {

    /**
     * Fetches the financial profile for the given user.
     *
     * @throws org.backendcompas.core.exception.NotFoundException if no profile exists for {@code userId}
     */
    StudentProfileResponseDto getProfile(UUID userId);

    /**
     * Creates a new profile if one does not yet exist for {@code userId},
     * or fully overwrites all fields if one already does.
     *
     * @throws org.backendcompas.core.exception.NotFoundException if {@code userId} does not correspond to any user
     * @return result carrying the saved profile and a flag indicating whether it was newly created
     */
    UpsertResult upsert(UUID userId, StudentProfileRequestDto dto);
}
