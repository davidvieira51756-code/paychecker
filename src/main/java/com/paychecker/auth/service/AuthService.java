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
import com.paychecker.auth.dto.LoginRequest;
import com.paychecker.auth.dto.LoginResponse;
import com.paychecker.auth.security.JwtService;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import com.paychecker.auth.security.LoginRateLimiter;

import java.util.Map;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EventLogService eventLogService;
    private final LoginRateLimiter loginRateLimiter;

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
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (loginRateLimiter.isBlocked(normalizedEmail)) {
            eventLogService.recordEvent(
                    EventType.RATE_LIMIT_TRIGGERED,
                    "AUTH",
                    0L,
                    Map.of(
                            "email", normalizedEmail,
                            "reason", "TOO_MANY_FAILED_LOGIN_ATTEMPTS"
                    )
            );

            throw new ResponseStatusException(
                    TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again later"
            );
        }

        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    loginRateLimiter.recordFailedAttempt(normalizedEmail);

                    eventLogService.recordEvent(
                            EventType.LOGIN_FAILED,
                            "AUTH",
                            0L,
                            Map.of(
                                    "email", normalizedEmail,
                                    "reason", "USER_NOT_FOUND"
                            )
                    );

                    return new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailedAttempt(normalizedEmail);

            eventLogService.recordEvent(
                    EventType.LOGIN_FAILED,
                    "USER",
                    user.getId(),
                    Map.of(
                            "email", user.getEmail(),
                            "reason", "INVALID_PASSWORD"
                    )
            );

            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            eventLogService.recordEvent(
                    EventType.LOGIN_FAILED,
                    "USER",
                    user.getId(),
                    Map.of(
                            "email", user.getEmail(),
                            "reason", "USER_NOT_ACTIVE",
                            "status", user.getStatus().name()
                    )
            );

            throw new ResponseStatusException(FORBIDDEN, "User account is not active");
        }

        loginRateLimiter.recordSuccessfulLogin(normalizedEmail);

        String token = jwtService.generateToken(user);

        eventLogService.recordEvent(
                EventType.LOGIN_SUCCESS,
                "USER",
                user.getId(),
                Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                )
        );

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                token,
                "Bearer",
                jwtService.getExpirationMinutes()
        );
    }
}