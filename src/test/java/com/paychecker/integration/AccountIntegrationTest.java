package com.paychecker.integration;

import com.paychecker.account.domain.AccountStatus;
import com.paychecker.account.dto.AccountResponse;
import com.paychecker.account.dto.CreateAccountRequest;
import com.paychecker.auth.dto.LoginRequest;
import com.paychecker.auth.dto.LoginResponse;
import com.paychecker.auth.dto.RegisterUserRequest;
import com.paychecker.auth.dto.UserResponse;
import com.paychecker.common.exception.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAccountSuccessfullyWhenAuthenticated() {
        HttpHeaders headers = authenticatedHeaders();

        CreateAccountRequest request = new CreateAccountRequest(
                "Integration User",
                "PT50010100000000000000101",
                "EUR",
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("5000.00")
        );

        HttpEntity<CreateAccountRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/accounts",
                entity,
                AccountResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        AccountResponse account = response.getBody();

        assertThat(account.id()).isNotNull();
        assertThat(account.ownerName()).isEqualTo("Integration User");
        assertThat(account.iban()).isEqualTo("PT50010100000000000000101");
        assertThat(account.currency()).isEqualTo("EUR");
        assertThat(account.balance()).isEqualByComparingTo("1000.00");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldReturnConflictWhenIbanAlreadyExists() {
        HttpHeaders headers = authenticatedHeaders();

        CreateAccountRequest request = new CreateAccountRequest(
                "Duplicate User",
                "PT50010200000000000000102",
                "EUR",
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("5000.00")
        );

        HttpEntity<CreateAccountRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity("/api/accounts", entity, AccountResponse.class);

        ResponseEntity<ApiErrorResponse> duplicateResponse = restTemplate.postForEntity(
                "/api/accounts",
                entity,
                ApiErrorResponse.class
        );

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody()).isNotNull();
        assertThat(duplicateResponse.getBody().message())
                .isEqualTo("An account with this IBAN already exists");
    }

    @Test
    void shouldReturnValidationErrorsWhenRequestIsInvalid() {
        HttpHeaders headers = authenticatedHeaders();

        CreateAccountRequest request = new CreateAccountRequest(
                "",
                "",
                "EU",
                new BigDecimal("-10.00"),
                new BigDecimal("-5.00"),
                new BigDecimal("-100.00")
        );

        HttpEntity<CreateAccountRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/accounts",
                entity,
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        ApiErrorResponse error = response.getBody();

        assertThat(error.message()).isEqualTo("Validation failed");
        assertThat(error.validationErrors()).containsKeys(
                "ownerName",
                "iban",
                "currency",
                "initialBalance",
                "dailyLimit",
                "monthlyLimit"
        );
    }

    @Test
    void shouldRejectAccountAccessWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/accounts",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    private HttpHeaders authenticatedHeaders() {
        String email = "integration-" + UUID.randomUUID() + "@example.com";
        String password = "Password123";

        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "Integration Test User",
                email,
                password
        );

        ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest loginRequest = new LoginRequest(email, password);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                loginRequest,
                LoginResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().accessToken()).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(loginResponse.getBody().accessToken());

        return headers;
    }
}