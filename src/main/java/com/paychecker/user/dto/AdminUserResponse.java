package com.paychecker.user.dto;

import com.paychecker.user.domain.UserRole;
import com.paychecker.user.domain.UserStatus;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}