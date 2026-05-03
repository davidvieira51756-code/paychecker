package com.paychecker.auth.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, LoginAttemptWindow> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        LoginAttemptWindow window = attempts.get(normalizeKey(key));

        if (window == null) {
            return false;
        }

        if (isExpired(window)) {
            attempts.remove(normalizeKey(key));
            return false;
        }

        return window.failedAttempts() >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailedAttempt(String key) {
        String normalizedKey = normalizeKey(key);

        attempts.compute(normalizedKey, (ignored, existingWindow) -> {
            if (existingWindow == null || isExpired(existingWindow)) {
                return new LoginAttemptWindow(1, Instant.now());
            }

            return new LoginAttemptWindow(
                    existingWindow.failedAttempts() + 1,
                    existingWindow.windowStart()
            );
        });
    }

    public void recordSuccessfulLogin(String key) {
        attempts.remove(normalizeKey(key));
    }

    private boolean isExpired(LoginAttemptWindow window) {
        return window.windowStart().plus(WINDOW).isBefore(Instant.now());
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase();
    }

    private record LoginAttemptWindow(
            int failedAttempts,
            Instant windowStart
    ) {
    }
}