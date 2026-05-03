package com.paychecker.user.service;

import com.paychecker.common.dto.PageResponse;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import com.paychecker.user.domain.AppUser;
import com.paychecker.user.domain.UserRole;
import com.paychecker.user.domain.UserStatus;
import com.paychecker.user.dto.AdminUserResponse;
import com.paychecker.user.dto.UpdateUserRoleRequest;
import com.paychecker.user.dto.UpdateUserStatusRequest;
import com.paychecker.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final AppUserRepository appUserRepository;
    private final EventLogService eventLogService;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(
                appUserRepository.findAll(pageable)
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        AppUser user = findUserById(id);

        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        AppUser user = findUserById(id);

        UserRole previousRole = user.getRole();

        user.setRole(request.role());

        AppUser savedUser = appUserRepository.save(user);

        eventLogService.recordEvent(
                EventType.USER_ROLE_UPDATED,
                "USER",
                savedUser.getId(),
                Map.of(
                        "email", savedUser.getEmail(),
                        "previousRole", previousRole.name(),
                        "newRole", savedUser.getRole().name()
                )
        );

        return toResponse(savedUser);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        AppUser user = findUserById(id);

        UserStatus previousStatus = user.getStatus();

        user.setStatus(request.status());

        AppUser savedUser = appUserRepository.save(user);

        eventLogService.recordEvent(
                EventType.USER_STATUS_UPDATED,
                "USER",
                savedUser.getId(),
                Map.of(
                        "email", savedUser.getEmail(),
                        "previousStatus", previousStatus.name(),
                        "newStatus", savedUser.getStatus().name()
                )
        );

        return toResponse(savedUser);
    }

    private AppUser findUserById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    private AdminUserResponse toResponse(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}