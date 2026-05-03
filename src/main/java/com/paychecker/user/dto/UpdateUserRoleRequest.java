package com.paychecker.user.dto;

import com.paychecker.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(

        @NotNull(message = "Role is required")
        UserRole role
) {
}