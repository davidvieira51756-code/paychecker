package com.paychecker.auth.dto;

import com.paychecker.user.domain.UserRole;
import com.paychecker.user.domain.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt
) {
}