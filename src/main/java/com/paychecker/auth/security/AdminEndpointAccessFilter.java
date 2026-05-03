package com.paychecker.auth.security;

import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminEndpointAccessFilter extends OncePerRequestFilter {

    private final EventLogService eventLogService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        recordAdminEndpointAccessIfNeeded(request);

        filterChain.doFilter(request, response);
    }

    private void recordAdminEndpointAccessIfNeeded(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/api/event-log")) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return;
        }

        try {
            eventLogService.recordEvent(
                    EventType.ADMIN_ENDPOINT_ACCESS,
                    "SECURITY",
                    0L,
                    Map.of(
                            "username", authentication.getName(),
                            "method", request.getMethod(),
                            "path", request.getRequestURI(),
                            "remoteAddress", request.getRemoteAddr()
                    )
            );
        } catch (Exception ignored) {
            // Request should not fail if audit logging fails.
        }
    }
}