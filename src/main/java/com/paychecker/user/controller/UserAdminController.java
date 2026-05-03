package com.paychecker.user.controller;

import com.paychecker.common.dto.PageResponse;
import com.paychecker.user.dto.AdminUserResponse;
import com.paychecker.user.dto.UpdateUserRoleRequest;
import com.paychecker.user.dto.UpdateUserStatusRequest;
import com.paychecker.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public PageResponse<AdminUserResponse> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return userAdminService.getAllUsers(pageable);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUserById(@PathVariable Long id) {
        return userAdminService.getUserById(id);
    }

    @PatchMapping("/{id}/role")
    public AdminUserResponse updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return userAdminService.updateUserRole(id, request);
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return userAdminService.updateUserStatus(id, request);
    }
}