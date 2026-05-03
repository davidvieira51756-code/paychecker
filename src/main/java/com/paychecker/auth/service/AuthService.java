package com.paychecker.auth.service;

import com.paychecker.auth.dto.RegisterUserRequest;
import com.paychecker.auth.dto.UserResponse;
import com.paychecker.user.domain.AppUser;
import com.paychecker.user.domain.UserRole;
import com.paychecker.user.domain.UserStatus;
import com.paychecker.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(CONFLICT, "A user with this email already exists");
        }

        AppUser user = AppUser.builder()
                .fullName(request.fullName())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}