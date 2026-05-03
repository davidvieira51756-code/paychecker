package com.paychecker.integration;

import com.paychecker.auth.dto.LoginRequest;
import com.paychecker.auth.dto.LoginResponse;
import com.paychecker.auth.dto.RegisterUserRequest;
import com.paychecker.auth.dto.UserResponse;
import com.paychecker.common.dto.PageResponse;
import com.paychecker.common.exception.ApiErrorResponse;
import com.paychecker.eventlog.dto.FinancialEventResponse;
import com.paychecker.user.domain.AppUser;
import com.paychecker.user.domain.UserRole;
import com.paychecker.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldReturnUnauthorizedJsonAndLogEventWhenNoTokenIsProvided() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/api/accounts",
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Authentication is required to access this resource");

        HttpHeaders adminHeaders = authenticatedHeaders(UserRole.ADMIN);

        ResponseEntity<PageResponse<FinancialEventResponse>> eventsResponse =
                restTemplate.exchange(
                        "/api/event-log?page=0&size=20",
                        HttpMethod.GET,
                        new HttpEntity<>(adminHeaders),
                        new ParameterizedTypeReference<>() {
                        }
                );

        assertThat(eventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(eventsResponse.getBody()).isNotNull();

        assertThat(eventsResponse.getBody().content())
                .extracting(event -> event.eventType().name())
                .contains("UNAUTHORIZED_ACCESS");
    }

    @Test
    void shouldReturnForbiddenJsonAndLogEventWhenCustomerAccessesAlerts() {
        HttpHeaders customerHeaders = authenticatedHeaders(UserRole.CUSTOMER);

        ResponseEntity<ApiErrorResponse> response =
                restTemplate.exchange(
                        "/api/alerts",
                        HttpMethod.GET,
                        new HttpEntity<>(customerHeaders),
                        ApiErrorResponse.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("You do not have permission to access this resource");

        HttpHeaders adminHeaders = authenticatedHeaders(UserRole.ADMIN);

        ResponseEntity<PageResponse<FinancialEventResponse>> eventsResponse =
                restTemplate.exchange(
                        "/api/event-log?page=0&size=20",
                        HttpMethod.GET,
                        new HttpEntity<>(adminHeaders),
                        new ParameterizedTypeReference<>() {
                        }
                );

        assertThat(eventsResponse.getBody()).isNotNull();

        assertThat(eventsResponse.getBody().content())
                .extracting(event -> event.eventType().name())
                .contains("ACCESS_DENIED");
    }

    @Test
    void shouldLogAdminEndpointAccessWhenAdminReadsEventLog() {
        HttpHeaders adminHeaders = authenticatedHeaders(UserRole.ADMIN);

        ResponseEntity<PageResponse<FinancialEventResponse>> response =
                restTemplate.exchange(
                        "/api/event-log?page=0&size=20",
                        HttpMethod.GET,
                        new HttpEntity<>(adminHeaders),
                        new ParameterizedTypeReference<>() {
                        }
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        assertThat(response.getBody().content())
                .extracting(event -> event.eventType().name())
                .contains("ADMIN_ENDPOINT_ACCESS");
    }

    @Test
    void shouldRateLimitLoginAfterTooManyFailedAttempts() {
        String email = "rate-limit-" + UUID.randomUUID() + "@example.com";
        String password = "Password123";

        registerUser(email, password, UserRole.CUSTOMER);

        LoginRequest wrongPasswordRequest = new LoginRequest(email, "WrongPassword");

        for (int i = 1; i <= 5; i++) {
            ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                    "/api/auth/login",
                    wrongPasswordRequest,
                    ApiErrorResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<ApiErrorResponse> rateLimitedResponse = restTemplate.postForEntity(
                "/api/auth/login",
                wrongPasswordRequest,
                ApiErrorResponse.class
        );

        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rateLimitedResponse.getBody()).isNotNull();
        assertThat(rateLimitedResponse.getBody().message())
                .isEqualTo("Too many failed login attempts. Please try again later");
    }

    private HttpHeaders authenticatedHeaders(UserRole role) {
        String email = "security-" + role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String password = "Password123";

        registerUser(email, password, role);

        LoginRequest loginRequest = new LoginRequest(email, password);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                loginRequest,
                LoginResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(loginResponse.getBody().accessToken());

        return headers;
    }

    private void registerUser(String email, String password, UserRole role) {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "Security Test User",
                email,
                password
        );

        ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow();

        user.setRole(role);
        appUserRepository.save(user);
    }
}