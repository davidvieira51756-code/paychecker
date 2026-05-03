package com.paychecker.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paychecker.common.exception.ApiErrorResponse;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final EventLogService eventLogService;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        recordUnauthorizedAccess(request, authException);

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication is required to access this resource",
                request.getRequestURI(),
                null
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private void recordUnauthorizedAccess(HttpServletRequest request, AuthenticationException exception) {
        try {
            eventLogService.recordEvent(
                    EventType.UNAUTHORIZED_ACCESS,
                    "SECURITY",
                    0L,
                    Map.of(
                            "method", request.getMethod(),
                            "path", request.getRequestURI(),
                            "remoteAddress", request.getRemoteAddr(),
                            "reason", exception.getClass().getSimpleName()
                    )
            );
        } catch (Exception ignored) {
            // Security response should not fail if audit logging fails.
        }
    }
}