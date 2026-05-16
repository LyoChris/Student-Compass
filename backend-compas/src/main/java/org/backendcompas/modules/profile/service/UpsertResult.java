package org.backendcompas.modules.profile.service;

import org.backendcompas.modules.profile.dto.StudentProfileResponseDto;

/**
 * Carries the result of an upsert operation so the controller can decide
 * whether to respond with 201 Created or 200 OK.
 */
public record UpsertResult(StudentProfileResponseDto profile, boolean wasCreated) {
}
