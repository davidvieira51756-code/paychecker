package com.paychecker.auth.dto;

import com.paychecker.user.domain.UserRole;
import com.paychecker.user.domain.UserStatus;

public record LoginResponse(
        Long userId,
        String fullName,
        String email,
        UserRole role,
        UserStatus status,
        String message
) {
}